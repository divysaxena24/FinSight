package com.finsight.simulator.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {

    public enum AccountType {
        SAVINGS,
        CURRENT,
        CLEARING
    }

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        LOCKED
    }

    private int id;
    private int userId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private String createdAt;

    public Account() {
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
    }

    public Account(int id, int userId, String accountNumber, AccountType accountType, BigDecimal balance, AccountStatus status, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.status = status != null ? status : AccountStatus.ACTIVE;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

