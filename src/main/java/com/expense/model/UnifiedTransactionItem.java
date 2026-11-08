package com.expense.model;

import java.math.BigDecimal;

public class UnifiedTransactionItem {

    public enum SourceType {
        SIMULATOR,
        MANUAL
    }

    private String date; // YYYY-MM-DD or YYYY-MM-DD HH:MM
    private String type; // Income, Expense, Transfer, Deposit, Withdrawal
    private String category;
    private String description;
    private BigDecimal amount;
    private boolean isCredit; // true if +, false if -
    private SourceType source;
    private String reference; // referenceNumber for simulator, id for manual
    private Integer manualId; // if manual transaction
    private String status; // COMPLETED, etc.

    public UnifiedTransactionItem() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isCredit() {
        return isCredit;
    }

    public void setCredit(boolean credit) {
        isCredit = credit;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getManualId() {
        return manualId;
    }

    public void setManualId(Integer manualId) {
        this.manualId = manualId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

