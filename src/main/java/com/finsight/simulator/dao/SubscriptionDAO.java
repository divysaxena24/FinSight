package com.finsight.simulator.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.expense.util.DBConnection;
import com.finsight.simulator.subscription.UserSubscription;

public class SubscriptionDAO {

    public List<UserSubscription> listForUser(int userId) {
        String sql = "SELECT id, user_id, catalog_code, display_name, plan_code, plan_name, "
                + "price_paise, cycle_months, cycle_days, start_date "
                + "FROM user_subscriptions WHERE user_id = ? ORDER BY display_name COLLATE NOCASE, id";
        List<UserSubscription> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (ps == null) {
                return rows;
            }
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    public UserSubscription findById(int id, int userId) {
        String sql = "SELECT id, user_id, catalog_code, display_name, plan_code, plan_name, "
                + "price_paise, cycle_months, cycle_days, start_date "
                + "FROM user_subscriptions WHERE id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (ps == null) {
                return null;
            }
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public UserSubscription findCatalog(int userId, String catalogCode) {
        String sql = "SELECT id, user_id, catalog_code, display_name, plan_code, plan_name, "
                + "price_paise, cycle_months, cycle_days, start_date "
                + "FROM user_subscriptions WHERE user_id = ? AND catalog_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (ps == null) {
                return null;
            }
            ps.setInt(1, userId);
            ps.setString(2, catalogCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insert(UserSubscription subscription) {
        String sql = "INSERT INTO user_subscriptions "
                + "(user_id, catalog_code, display_name, plan_code, plan_name, price_paise, "
                + "cycle_months, cycle_days, start_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null
                     ? con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : null) {
            if (ps == null) {
                return -1;
            }
            bind(ps, subscription);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean update(UserSubscription subscription) {
        String sql = "UPDATE user_subscriptions SET catalog_code = ?, display_name = ?, plan_code = ?, "
                + "plan_name = ?, price_paise = ?, cycle_months = ?, cycle_days = ?, start_date = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (ps == null) {
                return false;
            }
            int i = 1;
            if (subscription.getCatalogCode() == null || subscription.getCatalogCode().isBlank()) {
                ps.setNull(i++, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i++, subscription.getCatalogCode());
            }
            ps.setString(i++, subscription.getDisplayName());
            if (subscription.getPlanCode() == null || subscription.getPlanCode().isBlank()) {
                ps.setNull(i++, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i++, subscription.getPlanCode());
            }
            ps.setString(i++, subscription.getPlanName());
            ps.setLong(i++, subscription.getPricePaise());
            ps.setInt(i++, subscription.getCycleMonths());
            ps.setInt(i++, subscription.getCycleDays());
            ps.setString(i++, subscription.getStartDate());
            ps.setInt(i++, subscription.getId());
            ps.setInt(i, subscription.getUserId());
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM user_subscriptions WHERE id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (ps == null) {
                return false;
            }
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void bind(PreparedStatement ps, UserSubscription subscription) throws Exception {
        int i = 1;
        ps.setInt(i++, subscription.getUserId());
        if (subscription.getCatalogCode() == null || subscription.getCatalogCode().isBlank()) {
            ps.setNull(i++, java.sql.Types.VARCHAR);
        } else {
            ps.setString(i++, subscription.getCatalogCode());
        }
        ps.setString(i++, subscription.getDisplayName());
        if (subscription.getPlanCode() == null || subscription.getPlanCode().isBlank()) {
            ps.setNull(i++, java.sql.Types.VARCHAR);
        } else {
            ps.setString(i++, subscription.getPlanCode());
        }
        ps.setString(i++, subscription.getPlanName());
        ps.setLong(i++, subscription.getPricePaise());
        ps.setInt(i++, subscription.getCycleMonths());
        ps.setInt(i++, subscription.getCycleDays());
        ps.setString(i, subscription.getStartDate());
    }

    private UserSubscription map(ResultSet rs) throws Exception {
        UserSubscription row = new UserSubscription();
        row.setId(rs.getInt("id"));
        row.setUserId(rs.getInt("user_id"));
        row.setCatalogCode(rs.getString("catalog_code"));
        row.setDisplayName(rs.getString("display_name"));
        row.setPlanCode(rs.getString("plan_code"));
        row.setPlanName(rs.getString("plan_name"));
        row.setPricePaise(rs.getLong("price_paise"));
        row.setCycleMonths(rs.getInt("cycle_months"));
        row.setCycleDays(rs.getInt("cycle_days"));
        row.setStartDate(rs.getString("start_date"));
        return row;
    }
}
