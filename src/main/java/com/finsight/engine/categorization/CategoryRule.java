package com.finsight.engine.categorization;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.finsight.engine.model.ParsedTransaction;

/**
 * Encapsulates an individual categorization rule with priority, matching criteria, and confidence scoring.
 */
public class CategoryRule {

    private final String ruleId;
    private final String targetCategory;
    private final Pattern pattern;
    private final ParsedTransaction.TransactionType requiredType; // Optional filter (INCOME, EXPENSE, or null for any)
    private final boolean isTransfer;
    private final boolean isRefund;
    private final BigDecimal confidence;
    private final int priority; // Higher numbers evaluated first

    public CategoryRule(String ruleId,
                        String targetCategory,
                        String regex,
                        ParsedTransaction.TransactionType requiredType,
                        boolean isTransfer,
                        boolean isRefund,
                        BigDecimal confidence,
                        int priority) {
        this.ruleId = ruleId;
        this.targetCategory = targetCategory;
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.requiredType = requiredType;
        this.isTransfer = isTransfer;
        this.isRefund = isRefund;
        this.confidence = confidence != null ? confidence : new BigDecimal("0.75");
        this.priority = priority;
    }

    public boolean matches(String narration, ParsedTransaction.TransactionType type) {
        if (requiredType != null && type != null && requiredType != type) {
            return false;
        }
        if (narration == null || narration.trim().isEmpty()) {
            return false;
        }
        return pattern.matcher(narration).find();
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public boolean isTransfer() {
        return isTransfer;
    }

    public boolean isRefund() {
        return isRefund;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public int getPriority() {
        return priority;
    }
}

