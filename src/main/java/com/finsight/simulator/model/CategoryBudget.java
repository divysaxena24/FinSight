package com.finsight.simulator.model;

import java.math.BigDecimal;

public class CategoryBudget {

    private int id;
    private int userId;
    private String category;
    private BigDecimal monthlyLimit;
    private String monthYear; // e.g. "2026-11"
    private String createdAt;
    private BigDecimal currentSpent = BigDecimal.ZERO;
    private BigDecimal remaining = BigDecimal.ZERO;
    private double percentage = 0.0;
    private boolean exceeded = false;
    private BigDecimal excessAmount = BigDecimal.ZERO;

    public CategoryBudget() {}

    public CategoryBudget(int id, int userId, String category, BigDecimal monthlyLimit, String monthYear, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthYear = monthYear;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(BigDecimal currentSpent) {
        this.currentSpent = currentSpent != null ? currentSpent : BigDecimal.ZERO;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining != null ? remaining : BigDecimal.ZERO;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public boolean isExceeded() {
        return exceeded;
    }

    public void setExceeded(boolean exceeded) {
        this.exceeded = exceeded;
    }

    public BigDecimal getExcessAmount() {
        return excessAmount;
    }

    public void setExcessAmount(BigDecimal excessAmount) {
        this.excessAmount = excessAmount != null ? excessAmount : BigDecimal.ZERO;
    }
}

