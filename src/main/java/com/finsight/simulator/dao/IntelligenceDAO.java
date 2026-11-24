package com.finsight.simulator.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.expense.util.DBConnection;
import com.finsight.simulator.model.AnomalyAlert;
import com.finsight.simulator.model.AnomalyAlert.AlertStatus;
import com.finsight.simulator.model.CategoryBudget;

public class IntelligenceDAO {

    // --- Budgets ---
    public BigDecimal getBudgetLimitForMonth(int userId, String category, String monthYear) {
        String sql = "SELECT monthly_limit FROM budgets "
                + "WHERE user_id = ? AND month_year = ? "
                + "AND LOWER(TRIM(category)) = LOWER(TRIM(?)) LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setInt(1, userId);
            ps.setString(2, monthYear);
            ps.setString(3, category);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return BigDecimal.valueOf(rs.getDouble("monthly_limit"))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigDecimal getMonthlyCategorySpend(int userId, String category, String monthYear) {
        BigDecimal spend = BigDecimal.ZERO;

        String legacySql = "SELECT COALESCE(SUM(amount), 0) FROM transactions "
                + "WHERE user_id = ? AND LOWER(type) = 'expense' "
                + "AND LOWER(TRIM(category)) = LOWER(TRIM(?)) "
                + "AND substr(transaction_date, 1, 7) = ?";
        String withdrawalSql = "SELECT COALESCE(SUM(st.amount), 0) "
                + "FROM simulator_transactions st "
                + "JOIN accounts acc ON acc.id = st.source_account_id "
                + "WHERE acc.user_id = ? AND st.transaction_type IN ('WITHDRAWAL', 'TRANSFER') "
                + "AND LOWER(TRIM(st.category)) = LOWER(TRIM(?)) "
                + "AND substr(st.created_at, 1, 7) = ?";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return BigDecimal.ZERO;
            try (PreparedStatement ps = con.prepareStatement(legacySql)) {
                ps.setInt(1, userId);
                ps.setString(2, category);
                ps.setString(3, monthYear);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        spend = spend.add(BigDecimal.valueOf(rs.getDouble(1)));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(withdrawalSql)) {
                ps.setInt(1, userId);
                ps.setString(2, category);
                ps.setString(3, monthYear);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        spend = spend.add(BigDecimal.valueOf(rs.getDouble(1)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return spend.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public boolean saveOrUpdateBudget(int userId, String category, BigDecimal monthlyLimit, String monthYear) {
        String sql = "INSERT INTO budgets (user_id, category, monthly_limit, month_year) "
                + "VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(user_id, category, month_year) "
                + "DO UPDATE SET monthly_limit = excluded.monthly_limit";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setDouble(3, monthlyLimit.doubleValue());
            ps.setString(4, monthYear);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<CategoryBudget> getBudgetsForMonth(int userId, String monthYear) {
        List<CategoryBudget> list = new ArrayList<>();
        String sql = "SELECT id, user_id, category, monthly_limit, month_year, created_at FROM budgets WHERE user_id = ? AND month_year = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, userId);
            ps.setString(2, monthYear);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CategoryBudget cb = new CategoryBudget();
                    cb.setId(rs.getInt("id"));
                    cb.setUserId(rs.getInt("user_id"));
                    cb.setCategory(rs.getString("category"));
                    cb.setMonthlyLimit(BigDecimal.valueOf(rs.getDouble("monthly_limit")).setScale(2, java.math.RoundingMode.HALF_UP));
                    cb.setMonthYear(rs.getString("month_year"));
                    cb.setCreatedAt(rs.getString("created_at"));
                    list.add(cb);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- Anomaly Alerts ---
    public boolean recordAnomalyAlert(int userId, String txRef, String severity, String title, String reason, double riskScore) {
        String sql = "INSERT INTO anomaly_alerts (user_id, transaction_ref, severity, title, reason, risk_score, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'NEW')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setInt(1, userId);
            ps.setString(2, txRef);
            ps.setString(3, severity);
            ps.setString(4, title);
            ps.setString(5, reason);
            ps.setDouble(6, riskScore);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<AnomalyAlert> getAlertsForUser(int userId, int limit) {
        List<AnomalyAlert> list = new ArrayList<>();
        String sql = "SELECT id, user_id, transaction_ref, severity, title, reason, risk_score, status, created_at "
                + "FROM anomaly_alerts WHERE user_id = ? ORDER BY id DESC LIMIT ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, userId);
            ps.setInt(2, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AnomalyAlert a = new AnomalyAlert();
                    a.setId(rs.getInt("id"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setTransactionRef(rs.getString("transaction_ref"));
                    a.setSeverity(rs.getString("severity"));
                    a.setTitle(rs.getString("title"));
                    a.setReason(rs.getString("reason"));
                    a.setRiskScore(rs.getDouble("risk_score"));
                    a.setStatus(AlertStatus.valueOf(rs.getString("status")));
                    a.setCreatedAt(rs.getString("created_at"));
                    list.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean acknowledgeAlert(int alertId, int userId) {
        String sql = "UPDATE anomaly_alerts SET status = 'ACKNOWLEDGED' WHERE id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setInt(1, alertId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

