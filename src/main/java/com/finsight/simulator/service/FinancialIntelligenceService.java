package com.finsight.simulator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.finsight.engine.insights.BudgetRecommendation;
import com.finsight.engine.insights.BudgetRecommendationEngine;
import com.finsight.engine.insights.FinancialInsight;
import com.finsight.engine.insights.SpendingBaselineCalculator;
import com.finsight.engine.insights.UnusualTransactionDetector;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.IntelligenceDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.AnomalyAlert;
import com.finsight.simulator.model.CategoryBudget;
import com.finsight.simulator.model.FinancialHealthReport;
import com.finsight.simulator.model.SimulatorTransaction;

public class FinancialIntelligenceService {

    private final LedgerDAO ledgerDAO;
    private final AccountDAO accountDAO;
    private final IntelligenceDAO intelligenceDAO;
    private final SpendingBaselineCalculator baselineCalculator;
    private final UnusualTransactionDetector unusualDetector;
    private final BudgetRecommendationEngine budgetEngine;

    public FinancialIntelligenceService() {
        this(new LedgerDAO(), new AccountDAO(), new IntelligenceDAO());
    }

    public FinancialIntelligenceService(LedgerDAO ledgerDAO, AccountDAO accountDAO, IntelligenceDAO intelligenceDAO) {
        this.ledgerDAO = ledgerDAO;
        this.accountDAO = accountDAO;
        this.intelligenceDAO = intelligenceDAO;
        this.baselineCalculator = new SpendingBaselineCalculator(1); // Allow 1+ month baseline for responsive simulator
        this.unusualDetector = new UnusualTransactionDetector(this.baselineCalculator);
        this.budgetEngine = new BudgetRecommendationEngine(this.baselineCalculator, new BigDecimal("10.00"));
    }

    /**
     * Converts simulator transactions for a user into ParsedTransaction models
     * without confusing them with imported bank statements.
     */
    public List<ParsedTransaction> getUnifiedUserTransactions(int userId) {
        List<SimulatorTransaction> simTxs = ledgerDAO.getTransactionsForUser(userId, 500);
        List<Account> userAccounts = accountDAO.getAccountsByUserId(userId);
        Map<Integer, Boolean> userAccountIds = new HashMap<>();
        for (Account a : userAccounts) {
            userAccountIds.put(a.getId(), true);
        }

        List<ParsedTransaction> result = new ArrayList<>();
        for (SimulatorTransaction st : simTxs) {
            LocalDate date = LocalDate.now();
            if (st.getCreatedAt() != null && st.getCreatedAt().length() >= 10) {
                try {
                    date = LocalDate.parse(st.getCreatedAt().substring(0, 10));
                } catch (Exception ignored) {}
            }

            TransactionType pType = TransactionType.EXPENSE;
            boolean isDebit = false;
            boolean isCredit = false;

            if (st.getTransactionType() == SimulatorTransaction.TransactionType.DEPOSIT) {
                pType = TransactionType.INCOME;
                isCredit = true;
            } else if (st.getTransactionType() == SimulatorTransaction.TransactionType.WITHDRAWAL) {
                pType = TransactionType.EXPENSE;
                isDebit = true;
            } else if (st.getTransactionType() == SimulatorTransaction.TransactionType.TRANSFER) {
                boolean srcIsMine = st.getSourceAccountId() != null && userAccountIds.containsKey(st.getSourceAccountId());
                boolean dstIsMine = st.getDestinationAccountId() != null && userAccountIds.containsKey(st.getDestinationAccountId());

                if (srcIsMine && dstIsMine) {
                    // Internal transfer between own accounts: excluded from income/expense
                    pType = TransactionType.TRANSFER;
                } else if (srcIsMine) {
                    // Outgoing transfer to someone else: counts as an expense/transfer out
                    pType = TransactionType.EXPENSE;
                    isDebit = true;
                } else {
                    // Incoming transfer from someone else: counts as income/transfer in
                    pType = TransactionType.INCOME;
                    isCredit = true;
                }
            }

            BigDecimal amt = st.getAmount();
            ParsedTransaction pt = new ParsedTransaction(
                    st.getReferenceNumber(),
                    date,
                    st.getDescription() != null ? st.getDescription() : st.getTransactionType().name(),
                    isDebit ? amt : null,
                    isCredit ? amt : null,
                    amt,
                    pType,
                    null,
                    null,
                    st.getCategory() != null ? st.getCategory() : "Other",
                    BigDecimal.ONE,
                    st.getDescription(),
                    "SIMULATOR",
                    false,
                    false,
                    false,
                    false,
                    true
            );
            result.add(pt);
        }
        return result;
    }

    /**
     * Generates smart budget recommendations using deterministic historical spending.
     */
    public List<BudgetRecommendation> getSmartBudgetRecommendations(int userId) {
        List<ParsedTransaction> txs = getUnifiedUserTransactions(userId);
        if (txs.isEmpty()) {
            return Collections.emptyList();
        }
        return budgetEngine.generateRecommendations(txs, null, null);
    }

    /**
     * Evaluates a recently executed simulator transaction against user historical patterns.
     * Records an alert if an anomaly is detected.
     */
    public void evaluateAndRecordAnomaly(int userId, SimulatorTransaction tx) {
        if (tx == null || tx.getTransactionType() == SimulatorTransaction.TransactionType.DEPOSIT) {
            return;
        }

        List<ParsedTransaction> history = getUnifiedUserTransactions(userId);
        if (history.size() < 3) {
            return; // Need baseline to detect anomalies safely
        }

        List<FinancialInsight> insights = unusualDetector.detectAll(history, null, null);
        for (FinancialInsight insight : insights) {
            if (insight.getSupportingTransactionIds() != null && insight.getSupportingTransactionIds().contains(tx.getReferenceNumber())) {
                double score = "ALERT".equalsIgnoreCase(insight.getSeverity().name()) ? 85.0 : 60.0;
                intelligenceDAO.recordAnomalyAlert(userId, tx.getReferenceNumber(), insight.getSeverity().name(),
                        insight.getTitle(), insight.getExplanation(), score);
            }
        }
    }

    /**
     * Calculates deterministic financial health report (Score 0 - 100).
     */
    public FinancialHealthReport calculateFinancialHealth(int userId) {
        List<ParsedTransaction> txs = getUnifiedUserTransactions(userId);
        BigDecimal totalBalance = accountDAO.getTotalBalanceForUser(userId);

        if (txs.isEmpty()) {
            return FinancialHealthReport.insufficientData();
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (ParsedTransaction tx : txs) {
            if (tx.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(tx.getAmount());
            }
        }

        BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = BigDecimal.ZERO;
        BigDecimal expenseToIncomeRatio = BigDecimal.ZERO;

        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netCashFlow.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.00")).setScale(1, RoundingMode.HALF_UP);
            expenseToIncomeRatio = totalExpenses.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.00")).setScale(1, RoundingMode.HALF_UP);
        }

        // Calculate explainable score:
        int baseScore = 50;
        List<String> positiveHabits = new ArrayList<>();
        List<String> areasRequiringAttention = new ArrayList<>();
        List<String> opportunities = new ArrayList<>();

        // 1. Cash flow factor
        if (netCashFlow.compareTo(BigDecimal.ZERO) > 0) {
            baseScore += 20;
            positiveHabits.add("Positive net cash flow (+₹" + netCashFlow.setScale(2, RoundingMode.HALF_UP) + "). You earn more than you spend.");
        } else if (netCashFlow.compareTo(BigDecimal.ZERO) < 0) {
            baseScore -= 20;
            areasRequiringAttention.add("Negative net cash flow (-₹" + netCashFlow.abs().setScale(2, RoundingMode.HALF_UP) + "). Expenses exceed income.");
        }

        // 2. Savings rate factor
        if (savingsRate.compareTo(new BigDecimal("20.0")) >= 0) {
            baseScore += 15;
            positiveHabits.add("Healthy savings rate of " + savingsRate + "% (recommended > 20%).");
        } else if (savingsRate.compareTo(BigDecimal.ZERO) > 0) {
            baseScore += 5;
            opportunities.add("Current savings rate is " + savingsRate + "%. Aim to reach at least 20% by cutting discretionary spending.");
        } else {
            opportunities.add("Savings rate is 0% or negative. Establish an emergency reserve buffer.");
        }

        // 3. Liquidity factor
        if (totalBalance.compareTo(new BigDecimal("25000.00")) >= 0) {
            baseScore += 15;
            positiveHabits.add("Strong account liquidity (Available Balance: ₹" + totalBalance.setScale(2, RoundingMode.HALF_UP) + ").");
        } else if (totalBalance.compareTo(new BigDecimal("5000.00")) >= 0) {
            baseScore += 5;
        } else {
            areasRequiringAttention.add("Low total liquid balance (₹" + totalBalance.setScale(2, RoundingMode.HALF_UP) + "). Avoid overdraft risks.");
        }

        int finalScore = Math.max(0, Math.min(100, baseScore));
        String status;
        if (finalScore >= 80) status = "Excellent";
        else if (finalScore >= 65) status = "Good";
        else if (finalScore >= 50) status = "Fair";
        else status = "Needs Attention";

        return new FinancialHealthReport(
                finalScore, status, savingsRate, netCashFlow, expenseToIncomeRatio,
                positiveHabits, areasRequiringAttention, opportunities, true
        );
    }
}
