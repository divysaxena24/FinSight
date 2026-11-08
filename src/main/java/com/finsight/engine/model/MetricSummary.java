package com.finsight.engine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * High-level summary of financial metrics calculated from a statement.
 * All monetary amounts and rates use BigDecimal with scale 2 and RoundingMode.HALF_EVEN.
 */
public class MetricSummary {

    private final BigDecimal totalIncome;
    private final BigDecimal grossExpenses;
    private final BigDecimal totalRefunds;
    private final BigDecimal netExpenses;
    private final BigDecimal netCashFlow;
    private final BigDecimal savingsRate; // Percentage e.g. 25.50%
    private final BigDecimal averageIncomeDeposit; // Total Income / Income Count
    private final BigDecimal averageGrossExpense;  // Gross Expenses / Expense Count (Average ticket purchase size)
    private final BigDecimal averageDailySpend;    // Net Expenses / Statement Days
    private final BigDecimal highestExpense;

    private final int totalTransactionCount;       // In-period source transactions evaluated
    private final int incomeTransactionCount;
    private final int expenseTransactionCount;
    private final int refundTransactionCount;
    private final int transferTransactionCount;
    private final int suspectedDuplicateCount;
    private final BigDecimal suspectedDuplicateAmount;
    private final int confirmedDuplicateCount;
    private final BigDecimal confirmedDuplicateAmount;
    private final int outOfPeriodTransactionCount;
    private final boolean hasRefundExcess;         // true if totalRefunds > grossExpenses in current period

    public MetricSummary(BigDecimal totalIncome,
                         BigDecimal grossExpenses,
                         BigDecimal totalRefunds,
                         BigDecimal netExpenses,
                         BigDecimal netCashFlow,
                         BigDecimal savingsRate,
                         BigDecimal averageIncomeDeposit,
                         BigDecimal averageGrossExpense,
                         BigDecimal averageDailySpend,
                         BigDecimal highestExpense,
                         int totalTransactionCount,
                         int incomeTransactionCount,
                         int expenseTransactionCount,
                         int refundTransactionCount,
                         int transferTransactionCount,
                         int suspectedDuplicateCount,
                         BigDecimal suspectedDuplicateAmount,
                         int confirmedDuplicateCount,
                         BigDecimal confirmedDuplicateAmount,
                         int outOfPeriodTransactionCount,
                         boolean hasRefundExcess) {
        this.totalIncome = sanitize(totalIncome);
        this.grossExpenses = sanitize(grossExpenses);
        this.totalRefunds = sanitize(totalRefunds);
        this.netExpenses = sanitize(netExpenses);
        this.netCashFlow = sanitize(netCashFlow);
        this.savingsRate = sanitize(savingsRate);
        this.averageIncomeDeposit = sanitize(averageIncomeDeposit);
        this.averageGrossExpense = sanitize(averageGrossExpense);
        this.averageDailySpend = sanitize(averageDailySpend);
        this.highestExpense = sanitize(highestExpense);
        this.totalTransactionCount = totalTransactionCount;
        this.incomeTransactionCount = incomeTransactionCount;
        this.expenseTransactionCount = expenseTransactionCount;
        this.refundTransactionCount = refundTransactionCount;
        this.transferTransactionCount = transferTransactionCount;
        this.suspectedDuplicateCount = suspectedDuplicateCount;
        this.suspectedDuplicateAmount = sanitize(suspectedDuplicateAmount);
        this.confirmedDuplicateCount = confirmedDuplicateCount;
        this.confirmedDuplicateAmount = sanitize(confirmedDuplicateAmount);
        this.outOfPeriodTransactionCount = outOfPeriodTransactionCount;
        this.hasRefundExcess = hasRefundExcess;
    }

    private static BigDecimal sanitize(BigDecimal val) {
        if (val == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return val.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static MetricSummary empty() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        return new MetricSummary(
                zero, zero, zero, zero, zero, zero,
                zero, zero, zero, zero,
                0, 0, 0, 0, 0, 0, zero, 0, zero, 0, false
        );
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getGrossExpenses() {
        return grossExpenses;
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }

    public BigDecimal getNetExpenses() {
        return netExpenses;
    }

    /**
     * Backward-compatible alias for net expenses.
     */
    public BigDecimal getTotalExpenses() {
        return netExpenses;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    /**
     * Backward-compatible alias for net cash flow.
     */
    public BigDecimal getNetSavings() {
        return netCashFlow;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public BigDecimal getAverageIncomeDeposit() {
        return averageIncomeDeposit;
    }

    public BigDecimal getAverageGrossExpense() {
        return averageGrossExpense;
    }

    /**
     * Backward-compatible alias returning average gross expense per purchase.
     */
    public BigDecimal getAverageTransactionValue() {
        return averageGrossExpense;
    }

    public BigDecimal getAverageDailySpend() {
        return averageDailySpend;
    }

    public BigDecimal getHighestExpense() {
        return highestExpense;
    }

    public int getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public int getTransactionCount() {
        return totalTransactionCount;
    }

    public int getIncomeTransactionCount() {
        return incomeTransactionCount;
    }

    public int getExpenseTransactionCount() {
        return expenseTransactionCount;
    }

    public int getRefundTransactionCount() {
        return refundTransactionCount;
    }

    public int getTransferTransactionCount() {
        return transferTransactionCount;
    }

    public int getSuspectedDuplicateCount() {
        return suspectedDuplicateCount;
    }

    public BigDecimal getSuspectedDuplicateAmount() {
        return suspectedDuplicateAmount;
    }

    public int getConfirmedDuplicateCount() {
        return confirmedDuplicateCount;
    }

    public BigDecimal getConfirmedDuplicateAmount() {
        return confirmedDuplicateAmount;
    }

    public int getOutOfPeriodTransactionCount() {
        return outOfPeriodTransactionCount;
    }

    public boolean isHasRefundExcess() {
        return hasRefundExcess;
    }

    /**
     * Backward-compatible alias for hasRefundExcess.
     */
    public boolean isHasCrossPeriodRefunds() {
        return hasRefundExcess;
    }

    @Override
    public String toString() {
        return "MetricSummary{" +
                "income=" + totalIncome +
                ", grossExpenses=" + grossExpenses +
                ", refunds=" + totalRefunds +
                ", netExpenses=" + netExpenses +
                ", netCashFlow=" + netCashFlow +
                ", savingsRate=" + savingsRate + "%" +
                ", avgPurchase=" + averageGrossExpense +
                ", avgDaily=" + averageDailySpend +
                ", suspectedDuplicates=" + suspectedDuplicateCount +
                ", confirmedDuplicates=" + confirmedDuplicateCount +
                ", outOfPeriod=" + outOfPeriodTransactionCount +
                ", hasRefundExcess=" + hasRefundExcess +
                '}';
    }
}
