package com.finsight.engine.categorization;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Immutable container capturing the result of rule-based transaction categorization.
 */
public class CategorizationResult {

    private final String category;
    private final String normalizedMerchant;
    private final BigDecimal confidence; // 0.00 to 1.00
    private final boolean needsUserReview;
    private final boolean isTransfer;
    private final boolean isRefundOrReversal;
    private final String matchedRule;
    private final List<String> auditTrail;

    public CategorizationResult(String category,
                                String normalizedMerchant,
                                BigDecimal confidence,
                                boolean needsUserReview,
                                boolean isTransfer,
                                boolean isRefundOrReversal,
                                String matchedRule,
                                List<String> auditTrail) {
        this.category = (category != null && !category.trim().isEmpty()) ? category.trim() : "Other";
        this.normalizedMerchant = normalizedMerchant;
        this.confidence = confidence != null ? confidence : BigDecimal.ZERO;
        this.needsUserReview = needsUserReview;
        this.isTransfer = isTransfer;
        this.isRefundOrReversal = isRefundOrReversal;
        this.matchedRule = matchedRule != null ? matchedRule : "FALLBACK_DEFAULT";
        this.auditTrail = auditTrail != null ? Collections.unmodifiableList(auditTrail) : Collections.emptyList();
    }

    public static CategorizationResult unclassified(String fallbackMerchant, List<String> auditTrail) {
        return new CategorizationResult(
                "Other",
                fallbackMerchant,
                new BigDecimal("0.50"),
                true,
                false,
                false,
                "UNCLASSIFIED_FALLBACK",
                auditTrail
        );
    }

    public String getCategory() {
        return category;
    }

    public String getNormalizedMerchant() {
        return normalizedMerchant;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public boolean isNeedsUserReview() {
        return needsUserReview;
    }

    public boolean isTransfer() {
        return isTransfer;
    }

    public boolean isRefundOrReversal() {
        return isRefundOrReversal;
    }

    public String getMatchedRule() {
        return matchedRule;
    }

    public List<String> getAuditTrail() {
        return auditTrail;
    }

    @Override
    public String toString() {
        return "CategorizationResult{" +
                "category='" + category + '\'' +
                ", merchant='" + normalizedMerchant + '\'' +
                ", confidence=" + confidence +
                ", review=" + needsUserReview +
                ", transfer=" + isTransfer +
                ", refund=" + isRefundOrReversal +
                ", rule='" + matchedRule + '\'' +
                '}';
    }
}

