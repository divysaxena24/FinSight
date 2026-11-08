package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.finsight.engine.model.ParsedTransaction;

/**
 * Deterministic engine for generating category-level monthly budget recommendations.
 * 
 * Rules:
 * 1. Derives recommended monthly budget from observed historical spending baseline.
 * 2. Applies a configurable safety buffer (default: +10.00%).
 * 3. Does not generate recommendations for categories lacking sufficient history.
 * 4. Detects upward spending trends and adjusts recommendations with an explainable rationale.
 * 5. Explains the exact formula and baseline behind every recommendation.
 * 6. Never fabricates income or assumes unspecified user goals.
 */
public class BudgetRecommendationEngine {

    public static final BigDecimal DEFAULT_SAFETY_MARGIN_PERCENT = new BigDecimal("10.00"); // 10%
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final SpendingBaselineCalculator baselineCalculator;
    private final BigDecimal defaultSafetyMarginPercent;

    public BudgetRecommendationEngine() {
        this(new SpendingBaselineCalculator(), DEFAULT_SAFETY_MARGIN_PERCENT);
    }

    public BudgetRecommendationEngine(SpendingBaselineCalculator baselineCalculator,
                                      BigDecimal defaultSafetyMarginPercent) {
        this.baselineCalculator = Objects.requireNonNull(baselineCalculator, "baselineCalculator must not be null");
        this.defaultSafetyMarginPercent = Objects.requireNonNull(defaultSafetyMarginPercent, "defaultSafetyMarginPercent must not be null");
    }

    /**
     * Generates budget recommendations for all categories with sufficient history.
     */
    public List<BudgetRecommendation> generateRecommendations(List<ParsedTransaction> transactions,
                                                              LocalDate statementStart,
                                                              LocalDate statementEnd) {
        return generateRecommendations(transactions, statementStart, statementEnd, defaultSafetyMarginPercent);
    }

    /**
     * Generates budget recommendations using a custom safety margin percentage.
     */
    public List<BudgetRecommendation> generateRecommendations(List<ParsedTransaction> transactions,
                                                              LocalDate statementStart,
                                                              LocalDate statementEnd,
                                                              BigDecimal safetyMarginPercent) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal margin = safetyMarginPercent != null ? safetyMarginPercent : defaultSafetyMarginPercent;

        Map<String, Map<YearMonth, BigDecimal>> monthlySpends = baselineCalculator.calculateMonthlyCategorySpending(
                transactions, statementStart, statementEnd);

        List<YearMonth> eligibleFullMonths = baselineCalculator.getEligibleFullMonths(transactions, statementStart, statementEnd);

        List<BudgetRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<String, Map<YearMonth, BigDecimal>> entry : monthlySpends.entrySet()) {
            String category = entry.getKey();
            Map<YearMonth, BigDecimal> monthlyData = entry.getValue();

            int monthsCount;
            BigDecimal totalSpend;

            if (!eligibleFullMonths.isEmpty()) {
                monthsCount = eligibleFullMonths.size();
                if (monthsCount < baselineCalculator.getMinMonthsForBaseline()) {
                    continue; // Insufficient history
                }
                totalSpend = BigDecimal.ZERO;
                for (YearMonth ym : eligibleFullMonths) {
                    totalSpend = totalSpend.add(monthlyData.getOrDefault(ym, BigDecimal.ZERO));
                }
            } else {
                monthsCount = monthlyData.size();
                if (monthsCount < baselineCalculator.getMinMonthsForBaseline()) {
                    // Do not recommend budgets for categories without sufficient evidence
                    continue;
                }
                totalSpend = monthlyData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal avgSpend = totalSpend.divide(BigDecimal.valueOf(monthsCount), 2, RM);

            // Baseline cannot be negative
            if (avgSpend.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Check if spending is trending upwards
            boolean trendingUp = baselineCalculator.isSpendingTrendingUp(monthlyData);

            // Effective safety buffer: if trending up, add an extra 5% buffer with note
            BigDecimal effectiveMargin = trendingUp ? margin.add(new BigDecimal("5.00")) : margin;

            BigDecimal marginMultiplier = BigDecimal.ONE.add(
                    effectiveMargin.divide(new BigDecimal("100"), 4, RM));
            BigDecimal recommendedBudget = avgSpend.multiply(marginMultiplier).setScale(2, RM);

            String rationale = String.format(
                    "Observed baseline of %s/month across %d active months. " +
                    "Applied a %s%% safety buffer%s to arrive at recommended budget of %s.",
                    avgSpend.setScale(2, RM),
                    monthsCount,
                    effectiveMargin.setScale(1, RM),
                    trendingUp ? " (including +5% trend adjustment for rising expenditure)" : "",
                    recommendedBudget.setScale(2, RM)
            );

            recommendations.add(new BudgetRecommendation(
                    category,
                    avgSpend.setScale(2, RM),
                    effectiveMargin.setScale(2, RM),
                    recommendedBudget,
                    trendingUp,
                    true,
                    monthsCount,
                    rationale
            ));
        }

        // Sort alphabetically by category
        recommendations.sort(Comparator.comparing(BudgetRecommendation::getCategory));
        return Collections.unmodifiableList(recommendations);
    }
}

