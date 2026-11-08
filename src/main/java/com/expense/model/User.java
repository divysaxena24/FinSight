package com.expense.model;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String transactionPin;
    private int pinAttempts;
    private String pinLockedUntil;
    private String createdAt;

    public User() {
        super();
    }

    public User(int id, String name, String email, String password) {
        super();
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public boolean hasTransactionPin() {
        return transactionPin != null && !transactionPin.trim().isEmpty();
    }

    public int getPinAttempts() {
        return pinAttempts;
    }

    public void setPinAttempts(int pinAttempts) {
        this.pinAttempts = pinAttempts;
    }

    public String getPinLockedUntil() {
        return pinLockedUntil;
    }

    public void setPinLockedUntil(String pinLockedUntil) {
        this.pinLockedUntil = pinLockedUntil;
    }

    public boolean isPinLocked() {
        if (pinLockedUntil == null || pinLockedUntil.trim().isEmpty()) {
            return false;
        }
        try {
            java.time.LocalDateTime locked = java.time.LocalDateTime.parse(pinLockedUntil, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return java.time.LocalDateTime.now().isBefore(locked);
        } catch (Exception e) {
            return false;
        }
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
