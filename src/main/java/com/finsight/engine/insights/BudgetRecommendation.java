package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Encapsulates a monthly budget recommendation for an expense category.
 * Distinguishes observed baseline spending from recommended budget with transparent calculation rationale.
 */
public class BudgetRecommendation {

    private final String category;
    private final BigDecimal observedMonthlyBaseline;
    private final BigDecimal safetyMarginPercent; // e.g. 10.00 for 10%
    private final BigDecimal recommendedBudget;
    private final boolean spendingTrendingUp;
    private final boolean sufficientHistory;
    private final int historicalMonthsCount;
    private final String calculationRationale;

    public BudgetRecommendation(String category,
                                BigDecimal observedMonthlyBaseline,
                                BigDecimal safetyMarginPercent,
                                BigDecimal recommendedBudget,
                                boolean spendingTrendingUp,
                                boolean sufficientHistory,
                                int historicalMonthsCount,
                                String calculationRationale) {
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.observedMonthlyBaseline = Objects.requireNonNull(observedMonthlyBaseline, "observedMonthlyBaseline must not be null");
        this.safetyMarginPercent = Objects.requireNonNull(safetyMarginPercent, "safetyMarginPercent must not be null");
        this.recommendedBudget = Objects.requireNonNull(recommendedBudget, "recommendedBudget must not be null");
        this.spendingTrendingUp = spendingTrendingUp;
        this.sufficientHistory = sufficientHistory;
        this.historicalMonthsCount = historicalMonthsCount;
        this.calculationRationale = Objects.requireNonNull(calculationRationale, "calculationRationale must not be null");
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getObservedMonthlyBaseline() {
        return observedMonthlyBaseline;
    }

    public BigDecimal getSafetyMarginPercent() {
        return safetyMarginPercent;
    }

    public BigDecimal getRecommendedBudget() {
        return recommendedBudget;
    }

    public boolean isSpendingTrendingUp() {
        return spendingTrendingUp;
    }

    public boolean isSufficientHistory() {
        return sufficientHistory;
    }

    public int getHistoricalMonthsCount() {
        return historicalMonthsCount;
    }

    public String getCalculationRationale() {
        return calculationRationale;
    }

    @Override
    public String toString() {
        return "BudgetRecommendation{" +
                "category='" + category + '\'' +
                ", baseline=" + observedMonthlyBaseline +
                ", recommended=" + recommendedBudget +
                ", trendingUp=" + spendingTrendingUp +
                ", sufficientHistory=" + sufficientHistory +
                '}';
    }
}

