package com.finsight.engine.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.FinancialStatementReport.MerchantSpend;
import com.finsight.engine.model.FinancialStatementReport.MonthlyCashFlow;
import com.finsight.engine.model.MetricSummary;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Deterministic Financial Analytics Calculator.
 * 
 * Rules:
 * 1. 100% BigDecimal arithmetic with RoundingMode.HALF_EVEN (Banker's Rounding) to 2 decimal places.
 * 2. Out-of-Period Isolation:
 *    - When declared statement boundaries (declaredStart, declaredEnd) are supplied, ONLY transactions
 *      within that inclusive range are included in operating metrics, category/merchant totals, and daily/monthly aggregates.
 *    - Out-of-period transactions are preserved in getTransactions() and isolated in getOutOfPeriodTransactions().
 *    - When declared dates are omitted, the calculator falls back to transaction-derived min and max dates.
 * 3. Duplicate Handling:
 *    - Suspected duplicates (duplicateCandidate == true) remain in operational totals and are flagged for review.
 *    - Confirmed duplicates (confirmedDuplicate == true) are excluded from calculations to avoid double-counting.
 *    - All source rows remain preserved in getTransactions().
 * 4. Refunds & Reversals:
 *    - Tracked separately across Gross Expenses, Total Refunds, and Net Expenses.
 *    - Multi-dimensionally reconciled across categories, merchants, daily, and monthly flows.
 *    - If totalRefunds > grossExpenses in the current period, hasRefundExcess is set to true.
 *      (Does not claim a cross-period refund unless explicitly matched).
 */
public class AnalyticsCalculator {

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final int MONEY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /**
     * Calculates report using transaction-derived date fallback.
     */
    public FinancialStatementReport calculateReport(List<ParsedTransaction> transactions) {
        return calculateReport(transactions, null, null);
    }

    /**
     * Calculates report using explicit declared statement boundaries.
     *
     * @param transactions list of parsed statement transactions
     * @param declaredStart declared statement billing start date (inclusive, optional)
     * @param declaredEnd declared statement billing end date (inclusive, optional)
     * @return immutable FinancialStatementReport
     */
    public FinancialStatementReport calculateReport(List<ParsedTransaction> transactions,
                                                   LocalDate declaredStart,
                                                   LocalDate declaredEnd) {
        List<String> warnings = new ArrayList<>();

        if (declaredStart != null && declaredEnd != null && declaredStart.isAfter(declaredEnd)) {
            throw new IllegalArgumentException("Declared statement start date (" + declaredStart 
                    + ") cannot be after end date (" + declaredEnd + ").");
        }

        if (transactions == null || transactions.isEmpty()) {
            return new FinancialStatementReport(
                    MetricSummary.empty(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    declaredStart,
                    declaredEnd,
                    declaredStart,
                    declaredEnd,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    warnings
            );
        }

        BigDecimal totalIncome = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal grossExpenses = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal totalRefunds = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal highestExpense = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

        int totalTxCount = 0;
        int incomeCount = 0;
        int expenseCount = 0;
        int refundCount = 0;
        int transferCount = 0;

        int suspectedDuplicateCount = 0;
        BigDecimal suspectedDuplicateAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

        int confirmedDuplicateCount = 0;
        BigDecimal confirmedDuplicateAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

        Map<String, BigDecimal> categoryGross = new HashMap<>();
        Map<String, BigDecimal> categoryRefunds = new HashMap<>();

        Map<YearMonth, BigDecimal> monthlyIncome = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthlyGrossExpense = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthlyRefunds = new TreeMap<>();

        Map<LocalDate, BigDecimal> dailyGrossExpense = new TreeMap<>();
        Map<LocalDate, BigDecimal> dailyRefunds = new TreeMap<>();

        Map<String, MerchantAccumulator> merchantMap = new HashMap<>();

        List<ParsedTransaction> inPeriodTransactions = new ArrayList<>();
        List<ParsedTransaction> outOfPeriodTransactions = new ArrayList<>();

        LocalDate minInPeriodDate = null;
        LocalDate maxInPeriodDate = null;

        for (ParsedTransaction tx : transactions) {
            if (tx == null) {
                continue;
            }

            LocalDate txDate = tx.getTransactionDate();

            // Check whether transaction is within declared statement bounds
            boolean isOutOfPeriod = false;
            if (declaredStart != null && txDate != null && txDate.isBefore(declaredStart)) {
                isOutOfPeriod = true;
            }
            if (declaredEnd != null && txDate != null && txDate.isAfter(declaredEnd)) {
                isOutOfPeriod = true;
            }

            if (isOutOfPeriod) {
                outOfPeriodTransactions.add(tx);
                warnings.add("Transaction [" + tx.getId() + "] dated " + txDate 
                        + " is outside declared statement range [" + declaredStart + " to " + declaredEnd + "] and excluded from metrics.");
                continue; // Exclude out-of-period transaction from all statement financial aggregates
            }

            inPeriodTransactions.add(tx);
            totalTxCount++;

            BigDecimal amt = tx.getAmount() != null 
                    ? tx.getAmount().setScale(MONEY_SCALE, ROUNDING_MODE) 
                    : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

            // Track suspected duplicates
            if (tx.isDuplicateCandidate()) {
                suspectedDuplicateCount++;
                suspectedDuplicateAmount = suspectedDuplicateAmount.add(amt).setScale(MONEY_SCALE, ROUNDING_MODE);
            }

            // Exclude ONLY confirmed duplicates from calculation
            if (tx.isConfirmedDuplicate()) {
                confirmedDuplicateCount++;
                confirmedDuplicateAmount = confirmedDuplicateAmount.add(amt).setScale(MONEY_SCALE, ROUNDING_MODE);
                continue;
            }

            // Exclude personal account transfers
            if (isTransfer(tx)) {
                transferCount++;
                continue;
            }

            if (txDate != null) {
                if (minInPeriodDate == null || txDate.isBefore(minInPeriodDate)) minInPeriodDate = txDate;
                if (maxInPeriodDate == null || txDate.isAfter(maxInPeriodDate)) maxInPeriodDate = txDate;
            }

            String category = (tx.getCategory() != null && !tx.getCategory().trim().isEmpty())
                    ? tx.getCategory().trim() : null;
            String merchant = (tx.getMerchant() != null && !tx.getMerchant().trim().isEmpty())
                    ? tx.getMerchant().trim() : (tx.getDescription() != null ? tx.getDescription().trim() : "Unknown");

            if (tx.getTransactionType() == TransactionType.INCOME) {
                if (tx.isRefundOrReversal()) {
                    refundCount++;
                    totalRefunds = totalRefunds.add(amt).setScale(MONEY_SCALE, ROUNDING_MODE);

                    String refundCat = category != null ? category : "Refunds & Reversals";
                    categoryRefunds.put(refundCat, categoryRefunds.getOrDefault(refundCat, BigDecimal.ZERO).add(amt));

                    if (txDate != null) {
                        YearMonth ym = YearMonth.from(txDate);
                        monthlyRefunds.put(ym, monthlyRefunds.getOrDefault(ym, BigDecimal.ZERO).add(amt));
                        dailyRefunds.put(txDate, dailyRefunds.getOrDefault(txDate, BigDecimal.ZERO).add(amt));
                    }

                    merchantMap.computeIfAbsent(merchant, k -> new MerchantAccumulator(k)).addRefund(amt);
                } else {
                    incomeCount++;
                    totalIncome = totalIncome.add(amt).setScale(MONEY_SCALE, ROUNDING_MODE);

                    if (txDate != null) {
                        YearMonth ym = YearMonth.from(txDate);
                        monthlyIncome.put(ym, monthlyIncome.getOrDefault(ym, BigDecimal.ZERO).add(amt));
                    }
                }
            } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                expenseCount++;
                grossExpenses = grossExpenses.add(amt).setScale(MONEY_SCALE, ROUNDING_MODE);

                if (amt.compareTo(highestExpense) > 0) {
                    highestExpense = amt;
                }

                String expCat = category != null ? category : "Uncategorized";
                categoryGross.put(expCat, categoryGross.getOrDefault(expCat, BigDecimal.ZERO).add(amt));

                if (txDate != null) {
                    YearMonth ym = YearMonth.from(txDate);
                    monthlyGrossExpense.put(ym, monthlyGrossExpense.getOrDefault(ym, BigDecimal.ZERO).add(amt));
                    dailyGrossExpense.put(txDate, dailyGrossExpense.getOrDefault(txDate, BigDecimal.ZERO).add(amt));
                }

                merchantMap.computeIfAbsent(merchant, k -> new MerchantAccumulator(k)).addSpend(amt);
            }
        }

        // Net Expenses = Gross Expenses - Total Refunds
        BigDecimal netExpenses = grossExpenses.subtract(totalRefunds).setScale(MONEY_SCALE, ROUNDING_MODE);

        // Refund Excess check: refunds exceed current-period gross expenses
        boolean hasRefundExcess = totalRefunds.compareTo(grossExpenses) > 0;
        if (hasRefundExcess) {
            warnings.add("Total refunds (₹" + totalRefunds + ") exceed current-period gross expenses (₹" 
                    + grossExpenses + "), resulting in net negative expenditure for the period.");
        }

        // Net Cash Flow = Total Income - Net Expenses
        BigDecimal netCashFlow = totalIncome.subtract(netExpenses).setScale(MONEY_SCALE, ROUNDING_MODE);

        // Savings Rate = (Net Cash Flow / Total Income) * 100% (or 0.00% if Total Income <= 0)
        BigDecimal savingsRate = BigDecimal.ZERO.setScale(PERCENT_SCALE, ROUNDING_MODE);
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netCashFlow.divide(totalIncome, 4, ROUNDING_MODE)
                    .multiply(HUNDRED)
                    .setScale(PERCENT_SCALE, ROUNDING_MODE);
        }

        // Average Income Deposit = Total Income / incomeCount
        BigDecimal avgIncomeDeposit = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        if (incomeCount > 0) {
            avgIncomeDeposit = totalIncome.divide(BigDecimal.valueOf(incomeCount), MONEY_SCALE, ROUNDING_MODE);
        }

        // Average Gross Expense = Gross Expenses / expenseCount (Average purchase ticket size)
        BigDecimal avgGrossExpense = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        if (expenseCount > 0) {
            avgGrossExpense = grossExpenses.divide(BigDecimal.valueOf(expenseCount), MONEY_SCALE, ROUNDING_MODE);
        }

        // Determine Effective Dates for Daily Spending Calculation
        LocalDate effectiveStart = declaredStart != null ? declaredStart : minInPeriodDate;
        LocalDate effectiveEnd = declaredEnd != null ? declaredEnd : maxInPeriodDate;

        BigDecimal avgDailySpend = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        if (effectiveStart != null && effectiveEnd != null) {
            long days = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
            if (days > 0 && netExpenses.compareTo(BigDecimal.ZERO) > 0) {
                avgDailySpend = netExpenses.divide(BigDecimal.valueOf(days), MONEY_SCALE, ROUNDING_MODE);
            }
        }

        MetricSummary summary = new MetricSummary(
                totalIncome,
                grossExpenses,
                totalRefunds,
                netExpenses,
                netCashFlow,
                savingsRate,
                avgIncomeDeposit,
                avgGrossExpense,
                avgDailySpend,
                highestExpense,
                totalTxCount,
                incomeCount,
                expenseCount,
                refundCount,
                transferCount,
                suspectedDuplicateCount,
                suspectedDuplicateAmount,
                confirmedDuplicateCount,
                confirmedDuplicateAmount,
                outOfPeriodTransactions.size(),
                hasRefundExcess
        );

        // Reconcile Net Category Expenses
        Map<String, BigDecimal> categoryNet = new LinkedHashMap<>();
        Map<String, BigDecimal> allCategoryGross = new LinkedHashMap<>();
        Map<String, BigDecimal> allCategoryRefunds = new LinkedHashMap<>();

        Map<String, BigDecimal> combinedCategories = new HashMap<>(categoryGross);
        for (String refCat : categoryRefunds.keySet()) {
            combinedCategories.putIfAbsent(refCat, BigDecimal.ZERO);
        }

        for (String cat : combinedCategories.keySet()) {
            BigDecimal g = categoryGross.getOrDefault(cat, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal r = categoryRefunds.getOrDefault(cat, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal n = g.subtract(r).setScale(MONEY_SCALE, ROUNDING_MODE);

            allCategoryGross.put(cat, g);
            allCategoryRefunds.put(cat, r);
            categoryNet.put(cat, n);
        }

        // Category Percentages based on Net Expenses (if Net Expenses > 0)
        Map<String, BigDecimal> categoryPercentages = new LinkedHashMap<>();
        if (netExpenses.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : categoryNet.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = entry.getValue()
                            .divide(netExpenses, 4, ROUNDING_MODE)
                            .multiply(HUNDRED)
                            .setScale(PERCENT_SCALE, ROUNDING_MODE);
                    categoryPercentages.put(entry.getKey(), pct);
                } else {
                    categoryPercentages.put(entry.getKey(), BigDecimal.ZERO.setScale(PERCENT_SCALE, ROUNDING_MODE));
                }
            }
        }

        // Reconcile Monthly Cash Flows
        Map<YearMonth, MonthlyCashFlow> monthlyFlows = new LinkedHashMap<>();
        YearMonth startYm = effectiveStart != null ? YearMonth.from(effectiveStart) : (minInPeriodDate != null ? YearMonth.from(minInPeriodDate) : null);
        YearMonth endYm = effectiveEnd != null ? YearMonth.from(effectiveEnd) : (maxInPeriodDate != null ? YearMonth.from(maxInPeriodDate) : null);

        if (startYm != null && endYm != null) {
            YearMonth cur = startYm;
            while (!cur.isAfter(endYm)) {
                BigDecimal inc = monthlyIncome.getOrDefault(cur, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
                BigDecimal grossExp = monthlyGrossExpense.getOrDefault(cur, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
                BigDecimal ref = monthlyRefunds.getOrDefault(cur, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
                BigDecimal netExp = grossExp.subtract(ref).setScale(MONEY_SCALE, ROUNDING_MODE);
                BigDecimal netFlow = inc.subtract(netExp).setScale(MONEY_SCALE, ROUNDING_MODE);

                monthlyFlows.put(cur, new MonthlyCashFlow(cur, inc, grossExp, ref, netExp, netFlow));
                cur = cur.plusMonths(1);
            }
        }

        // Reconcile Daily Expenses (Net)
        Map<LocalDate, BigDecimal> dailyNet = new TreeMap<>();
        Map<LocalDate, BigDecimal> combinedDates = new HashMap<>(dailyGrossExpense);
        for (LocalDate d : dailyRefunds.keySet()) {
            combinedDates.putIfAbsent(d, BigDecimal.ZERO);
        }
        for (LocalDate d : combinedDates.keySet()) {
            BigDecimal g = dailyGrossExpense.getOrDefault(d, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal r = dailyRefunds.getOrDefault(d, BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);
            dailyNet.put(d, g.subtract(r).setScale(MONEY_SCALE, ROUNDING_MODE));
        }

        // Reconcile Top Merchants (Net Spend)
        List<MerchantSpend> topMerchants = new ArrayList<>();
        for (MerchantAccumulator acc : merchantMap.values()) {
            BigDecimal g = acc.grossSpend.setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal r = acc.refunds.setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal n = g.subtract(r).setScale(MONEY_SCALE, ROUNDING_MODE);
            topMerchants.add(new MerchantSpend(acc.merchant, g, r, n, acc.count));
        }
        topMerchants.sort(Comparator.comparing(MerchantSpend::getNetSpend).reversed());

        return new FinancialStatementReport(
                summary,
                categoryNet,
                allCategoryGross,
                allCategoryRefunds,
                categoryPercentages,
                monthlyFlows,
                dailyNet,
                topMerchants,
                declaredStart,
                declaredEnd,
                effectiveStart,
                effectiveEnd,
                new ArrayList<>(transactions),
                outOfPeriodTransactions,
                warnings
        );
    }

    private static boolean isTransfer(ParsedTransaction tx) {
        if (tx.getTransactionType() == TransactionType.TRANSFER) {
            return true;
        }
        String cat = tx.getCategory();
        return cat != null && (cat.equalsIgnoreCase("Transfer") || cat.equalsIgnoreCase("Transfers"));
    }

    private static class MerchantAccumulator {
        private final String merchant;
        private BigDecimal grossSpend = BigDecimal.ZERO;
        private BigDecimal refunds = BigDecimal.ZERO;
        private int count = 0;

        MerchantAccumulator(String merchant) {
            this.merchant = merchant;
        }

        void addSpend(BigDecimal amt) {
            grossSpend = grossSpend.add(amt);
            count++;
        }

        void addRefund(BigDecimal amt) {
            refunds = refunds.add(amt);
            count++;
        }
    }
}
