package com.finsight.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the complete financial analysis report derived from a bank statement.
 * Uses defensive copying to protect internal state.
 */
public class FinancialStatementReport {

    private final MetricSummary summary;
    private final Map<String, BigDecimal> categoryExpenses;     // Net category expenses
    private final Map<String, BigDecimal> categoryGrossExpenses;
    private final Map<String, BigDecimal> categoryRefunds;
    private final Map<String, BigDecimal> categoryPercentages;   // Based on Net Expenses
    private final Map<YearMonth, MonthlyCashFlow> monthlyCashFlows;
    private final Map<LocalDate, BigDecimal> dailyExpenses;      // Net daily expenses
    private final List<MerchantSpend> topMerchants;
    private final LocalDate declaredStartDate;
    private final LocalDate declaredEndDate;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;
    private final List<ParsedTransaction> transactions;           // All source transactions
    private final List<ParsedTransaction> outOfPeriodTransactions; // Transactions outside declared statement bounds
    private final List<String> warnings;

    public static class MonthlyCashFlow {
        private final YearMonth month;
        private final BigDecimal income;
        private final BigDecimal grossExpense;
        private final BigDecimal refunds;
        private final BigDecimal netExpense;
        private final BigDecimal netCashFlow;

        public MonthlyCashFlow(YearMonth month, BigDecimal income, BigDecimal grossExpense,
                               BigDecimal refunds, BigDecimal netExpense, BigDecimal netCashFlow) {
            this.month = month;
            this.income = income;
            this.grossExpense = grossExpense;
            this.refunds = refunds;
            this.netExpense = netExpense;
            this.netCashFlow = netCashFlow;
        }

        public YearMonth getMonth() {
            return month;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public BigDecimal getGrossExpense() {
            return grossExpense;
        }

        public BigDecimal getRefunds() {
            return refunds;
        }

        public BigDecimal getNetExpense() {
            return netExpense;
        }

        public BigDecimal getExpense() {
            return netExpense;
        }

        public BigDecimal getNet() {
            return netCashFlow;
        }

        public BigDecimal getNetCashFlow() {
            return netCashFlow;
        }
    }

    public static class MerchantSpend {
        private final String merchant;
        private final BigDecimal grossSpend;
        private final BigDecimal refundAmount;
        private final BigDecimal netSpend;
        private final int transactionCount;

        public MerchantSpend(String merchant, BigDecimal grossSpend, BigDecimal refundAmount,
                             BigDecimal netSpend, int transactionCount) {
            this.merchant = merchant;
            this.grossSpend = grossSpend;
            this.refundAmount = refundAmount;
            this.netSpend = netSpend;
            this.transactionCount = transactionCount;
        }

        public String getMerchant() {
            return merchant;
        }

        public BigDecimal getGrossSpend() {
            return grossSpend;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public BigDecimal getNetSpend() {
            return netSpend;
        }

        /**
         * Backward-compatible alias for net spend.
         */
        public BigDecimal getTotalSpend() {
            return netSpend;
        }

        public int getTransactionCount() {
            return transactionCount;
        }
    }

    public FinancialStatementReport(MetricSummary summary,
                                    Map<String, BigDecimal> categoryExpenses,
                                    Map<String, BigDecimal> categoryGrossExpenses,
                                    Map<String, BigDecimal> categoryRefunds,
                                    Map<String, BigDecimal> categoryPercentages,
                                    Map<YearMonth, MonthlyCashFlow> monthlyCashFlows,
                                    Map<LocalDate, BigDecimal> dailyExpenses,
                                    List<MerchantSpend> topMerchants,
                                    LocalDate declaredStartDate,
                                    LocalDate declaredEndDate,
                                    LocalDate effectiveStartDate,
                                    LocalDate effectiveEndDate,
                                    List<ParsedTransaction> transactions,
                                    List<ParsedTransaction> outOfPeriodTransactions,
                                    List<String> warnings) {
        this.summary = summary != null ? summary : MetricSummary.empty();
        this.categoryExpenses = categoryExpenses != null ? Collections.unmodifiableMap(categoryExpenses) : Collections.emptyMap();
        this.categoryGrossExpenses = categoryGrossExpenses != null ? Collections.unmodifiableMap(categoryGrossExpenses) : Collections.emptyMap();
        this.categoryRefunds = categoryRefunds != null ? Collections.unmodifiableMap(categoryRefunds) : Collections.emptyMap();
        this.categoryPercentages = categoryPercentages != null ? Collections.unmodifiableMap(categoryPercentages) : Collections.emptyMap();
        this.monthlyCashFlows = monthlyCashFlows != null ? Collections.unmodifiableMap(monthlyCashFlows) : Collections.emptyMap();
        this.dailyExpenses = dailyExpenses != null ? Collections.unmodifiableMap(dailyExpenses) : Collections.emptyMap();
        this.topMerchants = topMerchants != null ? Collections.unmodifiableList(topMerchants) : Collections.emptyList();
        this.declaredStartDate = declaredStartDate;
        this.declaredEndDate = declaredEndDate;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.transactions = transactions != null ? Collections.unmodifiableList(transactions) : Collections.emptyList();
        this.outOfPeriodTransactions = outOfPeriodTransactions != null ? Collections.unmodifiableList(outOfPeriodTransactions) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
    }

    public MetricSummary getSummary() {
        return summary;
    }

    public Map<String, BigDecimal> getCategoryExpenses() {
        return categoryExpenses;
    }

    public Map<String, BigDecimal> getCategoryGrossExpenses() {
        return categoryGrossExpenses;
    }

    public Map<String, BigDecimal> getCategoryRefunds() {
        return categoryRefunds;
    }

    public Map<String, BigDecimal> getCategoryPercentages() {
        return categoryPercentages;
    }

    public Map<YearMonth, MonthlyCashFlow> getMonthlyCashFlows() {
        return monthlyCashFlows;
    }

    public Map<LocalDate, BigDecimal> getDailyExpenses() {
        return dailyExpenses;
    }

    public List<MerchantSpend> getTopMerchants() {
        return topMerchants;
    }

    public LocalDate getDeclaredStartDate() {
        return declaredStartDate;
    }

    public LocalDate getDeclaredEndDate() {
        return declaredEndDate;
    }

    public LocalDate getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public LocalDate getEffectiveEndDate() {
        return effectiveEndDate;
    }

    /**
     * Backward-compatible alias for effective start date.
     */
    public LocalDate getStartDate() {
        return effectiveStartDate;
    }

    /**
     * Backward-compatible alias for effective end date.
     */
    public LocalDate getEndDate() {
        return effectiveEndDate;
    }

    public List<ParsedTransaction> getTransactions() {
        return transactions;
    }

    public List<ParsedTransaction> getOutOfPeriodTransactions() {
        return outOfPeriodTransactions;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
