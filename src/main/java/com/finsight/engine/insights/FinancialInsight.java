package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates an explainable financial insight produced by the intelligence engine.
 * Every insight provides traceable supporting transaction IDs and calculation evidence.
 */
public class FinancialInsight {

    public enum InsightType {
        UNUSUAL_EXPENSE,
        CATEGORY_SPENDING_SPIKE,
        POTENTIAL_DUPLICATE,
        RECURRING_PAYMENT,
        INSUFFICIENT_DATA,
        SPENDING_TREND
    }

    public enum Severity {
        INFO,
        WARNING,
        ALERT
    }

    private final String id;
    private final InsightType type;
    private final Severity severity;
    private final String title;
    private final String explanation;
    private final String category;
    private final String merchant;
    private final BigDecimal amount;
    private final List<String> supportingTransactionIds;
    private final String calculationEvidence;
    private final String reviewRecommendation;

    public FinancialInsight(String id,
                            InsightType type,
                            Severity severity,
                            String title,
                            String explanation,
                            String category,
                            String merchant,
                            BigDecimal amount,
                            List<String> supportingTransactionIds,
                            String calculationEvidence,
                            String reviewRecommendation) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
        this.category = category;
        this.merchant = merchant;
        this.amount = amount;
        this.supportingTransactionIds = supportingTransactionIds != null
                ? Collections.unmodifiableList(supportingTransactionIds)
                : Collections.emptyList();
        this.calculationEvidence = calculationEvidence;
        this.reviewRecommendation = reviewRecommendation;
    }

    public String getId() {
        return id;
    }

    public InsightType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getCategory() {
        return category;
    }

    public String getMerchant() {
        return merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<String> getSupportingTransactionIds() {
        return supportingTransactionIds;
    }

    public String getCalculationEvidence() {
        return calculationEvidence;
    }

    public String getReviewRecommendation() {
        return reviewRecommendation;
    }

    @Override
    public String toString() {
        return "FinancialInsight{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", merchant='" + merchant + '\'' +
                ", amount=" + amount +
                '}';
    }
}

