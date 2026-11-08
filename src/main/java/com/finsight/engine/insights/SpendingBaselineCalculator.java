package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Calculates category-level and overall monthly spending baselines from historical transactions.
 * 
 * Rules & Accounting semantics:
 * 1. Considers strictly EXPENSE transactions and eligible refunds/reversals (credits against expenses).
 * 2. Excludes INCOME and TRANSFER transactions.
 * 3. Excludes confirmed duplicates (confirmedDuplicate == true).
 * 4. Preserves suspected duplicates (duplicateCandidate == true) unless confirmed.
 * 5. Respects declared statement boundary dates if provided.
 * 6. Explicitly checks and flags insufficient history (minimum months required).
 */
public class SpendingBaselineCalculator {

    public static final int DEFAULT_MIN_MONTHS_FOR_BASELINE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final int minMonthsForBaseline;

    public SpendingBaselineCalculator() {
        this(DEFAULT_MIN_MONTHS_FOR_BASELINE);
    }

    public SpendingBaselineCalculator(int minMonthsForBaseline) {
        if (minMonthsForBaseline < 1) {
            throw new IllegalArgumentException("minMonthsForBaseline must be >= 1");
        }
        this.minMonthsForBaseline = minMonthsForBaseline;
    }

    public int getMinMonthsForBaseline() {
        return minMonthsForBaseline;
    }

    /**
     * Calculates the net monthly historical spending baseline per category.
     * Net category spending per month = Gross Expenses in category - Refunds/Reversals in category.
     * 
     * When statement bounds (statementStart and statementEnd) are provided, any full calendar months
     * in the inclusive period where spending is 0.00 are included in the denominator as zero-spending months.
     * If statement bounds are not provided, only observed active months with transactions are used.
     * Partial first/last statement months (where statement start is not day 1 or statement end is not last day of month)
     * are evaluated consistently: full months within the boundary establish the monthly rate, preventing partial months
     * from diluting monthly baselines.
     */
    public Map<String, BigDecimal> calculateCategoryBaselines(List<ParsedTransaction> transactions,
                                                             LocalDate statementStart,
                                                             LocalDate statementEnd) {
        Map<String, Map<YearMonth, BigDecimal>> monthlyCategorySpends = calculateMonthlyCategorySpending(
                transactions, statementStart, statementEnd);

        // Determine all eligible full months within the declared statement boundary
        List<YearMonth> eligibleFullMonths = getEligibleFullMonths(transactions, statementStart, statementEnd);

        Map<String, BigDecimal> baselines = new HashMap<>();
        for (Map.Entry<String, Map<YearMonth, BigDecimal>> entry : monthlyCategorySpends.entrySet()) {
            String category = entry.getKey();
            Map<YearMonth, BigDecimal> monthlyData = entry.getValue();

            // If we have explicit statement boundaries and full eligible months >= 1
            int monthsCount;
            BigDecimal total;

            if (!eligibleFullMonths.isEmpty()) {
                // Denominator includes all eligible full months (including zero-spend months for this category)
                monthsCount = eligibleFullMonths.size();
                if (monthsCount < minMonthsForBaseline) {
                    continue; // Insufficient history
                }
                total = BigDecimal.ZERO;
                for (YearMonth ym : eligibleFullMonths) {
                    BigDecimal mSpend = monthlyData.getOrDefault(ym, BigDecimal.ZERO);
                    total = total.add(mSpend);
                }
            } else {
                // Fallback to observed active months
                monthsCount = monthlyData.size();
                if (monthsCount < minMonthsForBaseline) {
                    continue; // Insufficient history
                }
                total = monthlyData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal avg = total.divide(BigDecimal.valueOf(monthsCount), 2, RM);
            // Baseline cannot be negative (e.g. if refunds exceeded expenses in a rare month)
            baselines.put(category, avg.max(BigDecimal.ZERO).setScale(2, RM));
        }

        return Collections.unmodifiableMap(baselines);
    }

    /**
     * Determines eligible full calendar months within statement period.
     * Partial months (where statement begins after 1st or ends before last day) are excluded from the baseline
     * denominator if there are full months available, avoiding artificial dilution of monthly rates.
     */
    public List<YearMonth> getEligibleFullMonths(List<ParsedTransaction> transactions,
                                                LocalDate statementStart,
                                                LocalDate statementEnd) {
        if (statementStart == null || statementEnd == null || statementStart.isAfter(statementEnd)) {
            return Collections.emptyList();
        }

        YearMonth startYm = YearMonth.from(statementStart);
        YearMonth endYm = YearMonth.from(statementEnd);

        List<YearMonth> list = new ArrayList<>();
        YearMonth current = startYm;

        while (!current.isAfter(endYm)) {
            // Check if current is a full month relative to statement boundaries
            boolean isFullMonth = true;
            if (current.equals(startYm) && statementStart.getDayOfMonth() > 1) {
                isFullMonth = false;
            }
            if (current.equals(endYm) && statementEnd.getDayOfMonth() < current.lengthOfMonth()) {
                isFullMonth = false;
            }

            if (isFullMonth) {
                list.add(current);
            }
            current = current.plusMonths(1);
        }

        return list;
    }

    /**
     * Aggregates net spending grouped by Category and YearMonth.
     */
    public Map<String, Map<YearMonth, BigDecimal>> calculateMonthlyCategorySpending(
            List<ParsedTransaction> transactions,
            LocalDate statementStart,
            LocalDate statementEnd) {

        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<YearMonth, BigDecimal>> map = new HashMap<>();

        for (ParsedTransaction tx : transactions) {
            if (tx == null) continue;

            // 1. Confirmed duplicate exclusion
            if (tx.isConfirmedDuplicate()) {
                continue;
            }

            // 2. Out-of-period boundary exclusion
            LocalDate d = tx.getTransactionDate();
            if (d != null) {
                if (statementStart != null && d.isBefore(statementStart)) continue;
                if (statementEnd != null && d.isAfter(statementEnd)) continue;
            }

            // 3. Transfer exclusion
            if (tx.getTransactionType() == TransactionType.TRANSFER) {
                continue;
            }

            YearMonth ym = d != null ? YearMonth.from(d) : YearMonth.now();
            String category = tx.getCategory() != null && !tx.getCategory().trim().isEmpty()
                    ? tx.getCategory().trim() : "Other";

            BigDecimal netDelta = BigDecimal.ZERO;

            if (tx.getTransactionType() == TransactionType.EXPENSE) {
                BigDecimal gross = tx.getDebit() != null ? tx.getDebit()
                        : (tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO);
                netDelta = gross;
            } else if (tx.getTransactionType() == TransactionType.INCOME && tx.isRefundOrReversal()) {
                // Refund credits reduce net expense
                BigDecimal refund = tx.getCredit() != null ? tx.getCredit()
                        : (tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO);
                netDelta = refund.negate();
            }

            if (netDelta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            map.computeIfAbsent(category, k -> new HashMap<>())
               .merge(ym, netDelta, BigDecimal::add);
        }

        // Clean up: scale values to 2 decimal places
        for (Map<YearMonth, BigDecimal> monthly : map.values()) {
            for (Map.Entry<YearMonth, BigDecimal> entry : monthly.entrySet()) {
                entry.setValue(entry.getValue().setScale(2, RM));
            }
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Checks if a category has sufficient historical months.
     */
    public boolean hasSufficientHistory(Map<String, Map<YearMonth, BigDecimal>> monthlyCategorySpends, String category) {
        if (monthlyCategorySpends == null || category == null) return false;
        Map<YearMonth, BigDecimal> monthly = monthlyCategorySpends.get(category);
        return monthly != null && monthly.size() >= minMonthsForBaseline;
    }

    /**
     * Evaluates if category spending is trending upwards over available sequential months.
     * Returns true if there are at least 2 consecutive months of net spending growth.
     */
    public boolean isSpendingTrendingUp(Map<YearMonth, BigDecimal> monthlySpends) {
        if (monthlySpends == null || monthlySpends.size() < 2) {
            return false;
        }

        List<YearMonth> sortedMonths = new ArrayList<>(monthlySpends.keySet());
        Collections.sort(sortedMonths);

        if (sortedMonths.size() < 2) return false;

        // If 2 months: month2 > month1 by at least 5%
        if (sortedMonths.size() == 2) {
            BigDecimal m1 = monthlySpends.get(sortedMonths.get(0));
            BigDecimal m2 = monthlySpends.get(sortedMonths.get(1));
            return m1.compareTo(BigDecimal.ZERO) > 0 
                    && m2.compareTo(m1.multiply(new BigDecimal("1.05"))) > 0;
        }

        // For 3+ months: check if latest month > previous month by at least 5% and previous month > month prior by at least 5%
        // Significant consecutive upward trend
        int n = sortedMonths.size();
        BigDecimal last = monthlySpends.get(sortedMonths.get(n - 1));
        BigDecimal prev = monthlySpends.get(sortedMonths.get(n - 2));
        BigDecimal prev2 = monthlySpends.get(sortedMonths.get(n - 3));

        BigDecimal minPrev = prev2.multiply(new BigDecimal("1.05"));
        BigDecimal minLast = prev.multiply(new BigDecimal("1.05"));

        return prev2.compareTo(BigDecimal.ZERO) > 0 
                && prev.compareTo(minPrev) >= 0 
                && last.compareTo(minLast) >= 0;
    }
}

