package com.finsight.simulator.model;

public class AnomalyAlert {

    public enum AlertStatus {
        NEW,
        ACKNOWLEDGED
    }

    private int id;
    private int userId;
    private String transactionRef;
    private String severity;
    private String title;
    private String reason;
    private double riskScore;
    private AlertStatus status;
    private String createdAt;

    public AnomalyAlert() {
        this.status = AlertStatus.NEW;
    }

    public AnomalyAlert(int id, int userId, String transactionRef, String severity, String title, String reason, double riskScore, AlertStatus status, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.transactionRef = transactionRef;
        this.severity = severity;
        this.title = title;
        this.reason = reason;
        this.riskScore = riskScore;
        this.status = status != null ? status : AlertStatus.NEW;
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

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

