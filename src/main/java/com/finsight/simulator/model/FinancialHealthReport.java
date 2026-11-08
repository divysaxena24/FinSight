package com.finsight.simulator.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class FinancialHealthReport {

    private int score; // 0 to 100
    private String statusCategory; // Excellent, Good, Fair, Needs Attention, Insufficient Data
    private BigDecimal savingsRate;
    private BigDecimal monthlyCashFlow;
    private BigDecimal expenseToIncomeRatio;
    private List<String> positiveHabits;
    private List<String> areasRequiringAttention;
    private List<String> improvementOpportunities;
    private boolean hasSufficientData;

    public FinancialHealthReport() {
        this.positiveHabits = Collections.emptyList();
        this.areasRequiringAttention = Collections.emptyList();
        this.improvementOpportunities = Collections.emptyList();
    }

    public FinancialHealthReport(int score, String statusCategory, BigDecimal savingsRate,
                                 BigDecimal monthlyCashFlow, BigDecimal expenseToIncomeRatio,
                                 List<String> positiveHabits, List<String> areasRequiringAttention,
                                 List<String> improvementOpportunities, boolean hasSufficientData) {
        this.score = score;
        this.statusCategory = statusCategory;
        this.savingsRate = savingsRate;
        this.monthlyCashFlow = monthlyCashFlow;
        this.expenseToIncomeRatio = expenseToIncomeRatio;
        this.positiveHabits = positiveHabits != null ? positiveHabits : Collections.emptyList();
        this.areasRequiringAttention = areasRequiringAttention != null ? areasRequiringAttention : Collections.emptyList();
        this.improvementOpportunities = improvementOpportunities != null ? improvementOpportunities : Collections.emptyList();
        this.hasSufficientData = hasSufficientData;
    }

    public static FinancialHealthReport insufficientData() {
        return new FinancialHealthReport(0, "Insufficient Data", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList(),
                List.of("Not enough transaction history to evaluate financial health. Complete deposits, expenses, or transfers to establish health metrics."),
                List.of("Perform simulated transactions or upload monthly statements to build financial health indicators."),
                false);
    }

    public int getScore() {
        return score;
    }

    public String getStatusCategory() {
        return statusCategory;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public BigDecimal getMonthlyCashFlow() {
        return monthlyCashFlow;
    }

    public BigDecimal getExpenseToIncomeRatio() {
        return expenseToIncomeRatio;
    }

    public List<String> getPositiveHabits() {
        return positiveHabits;
    }

    public List<String> getAreasRequiringAttention() {
        return areasRequiringAttention;
    }

    public List<String> getImprovementOpportunities() {
        return improvementOpportunities;
    }

    public boolean isHasSufficientData() {
        return hasSufficientData;
    }
}

