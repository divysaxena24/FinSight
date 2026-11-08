package com.finsight.simulator.model;

import java.math.BigDecimal;

public class CategoryBudget {

    private int id;
    private int userId;
    private String category;
    private BigDecimal monthlyLimit;
    private String monthYear; // e.g. "2026-11"
    private String createdAt;

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
}

