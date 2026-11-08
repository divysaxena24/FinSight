package com.finsight.simulator.dao;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.expense.util.DBConnection;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.Account.AccountStatus;
import com.finsight.simulator.model.Account.AccountType;

public class AccountDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Account createAccount(int userId, AccountType accountType, BigDecimal initialDeposit) {
        String accountNumber = generateUniqueAccountNumber();

        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, status) VALUES (?, ?, ?, ?, 'ACTIVE')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : null) {

            if (con == null || ps == null) return null;

            ps.setInt(1, userId);
            ps.setString(2, accountNumber);
            ps.setString(3, accountType.name());
            ps.setDouble(4, initialDeposit != null ? initialDeposit.doubleValue() : 0.0);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        return getAccountById(id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Account getAccountById(int accountId) {
        String sql = "SELECT id, user_id, account_number, account_type, balance, status, created_at FROM accounts WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Account getAccountByNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) return null;
        String sql = "SELECT id, user_id, account_number, account_type, balance, status, created_at FROM accounts WHERE UPPER(account_number) = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setString(1, accountNumber.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final String CLEARING_ACCOUNT_NUMBER = "FS-SYSTEM-CLEARING";

    public Account getOrCreateClearingAccount(Connection con) throws SQLException {
        Account clearing = getAccountByNumber(CLEARING_ACCOUNT_NUMBER);
        if (clearing != null) {
            return clearing;
        }

        String sql = "INSERT OR IGNORE INTO accounts (id, user_id, account_number, account_type, balance, status) "
                + "VALUES (999999, 0, ?, 'CLEARING', 0.00, 'ACTIVE')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, CLEARING_ACCOUNT_NUMBER);
            ps.executeUpdate();
        }
        return getAccountByNumber(CLEARING_ACCOUNT_NUMBER);
    }

    public List<Account> getAccountsByUserId(int userId) {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT id, user_id, account_number, account_type, balance, status, created_at "
                + "FROM accounts WHERE user_id = ? AND account_type != 'CLEARING' ORDER BY id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAccount(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public BigDecimal getTotalBalanceForUser(int userId) {
        String sql = "SELECT SUM(balance) FROM accounts WHERE user_id = ? AND status = 'ACTIVE'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return BigDecimal.ZERO;
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double sum = rs.getDouble(1);
                    return BigDecimal.valueOf(sum).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public boolean updateBalance(Connection con, int accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, newBalance.doubleValue());
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        }
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < 20; i++) {
            long randomDigits = 1000000000L + (long)(RANDOM.nextDouble() * 9000000000L);
            String candidate = "FS-" + randomDigits;
            if (getAccountByNumber(candidate) == null) {
                return candidate;
            }
        }
        return "FS-" + System.currentTimeMillis();
    }

    private Account mapAccount(ResultSet rs) throws Exception {
        Account acc = new Account();
        acc.setId(rs.getInt("id"));
        acc.setUserId(rs.getInt("user_id"));
        acc.setAccountNumber(rs.getString("account_number"));
        acc.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        acc.setBalance(BigDecimal.valueOf(rs.getDouble("balance")).setScale(2, java.math.RoundingMode.HALF_UP));
        acc.setStatus(AccountStatus.valueOf(rs.getString("status")));
        acc.setCreatedAt(rs.getString("created_at"));
        return acc;
    }
}

