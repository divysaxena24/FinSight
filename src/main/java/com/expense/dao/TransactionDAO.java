package com.expense.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.expense.model.Transaction;
import com.expense.util.DBConnection;

public class TransactionDAO {

    // Add Transaction
    public boolean addTransaction(Transaction transaction) {
        if (transaction == null) {
            return false;
        }

        String sql = "INSERT INTO transactions(user_id, type, category, amount, description, transaction_date) "
                   + "VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            ps.setInt(1, transaction.getUserId());
            ps.setString(2, transaction.getType());
            ps.setString(3, transaction.getCategory());
            ps.setDouble(4, transaction.getAmount());
            ps.setString(5, transaction.getDescription());

            String dateStr = transaction.getTransactionDate() != null
                    ? transaction.getTransactionDate().toString()
                    : new Date(System.currentTimeMillis()).toString();
            ps.setString(6, dateStr);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // View Transactions for a user
    public List<Transaction> getTransactionsByUser(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, user_id, type, category, amount, description, transaction_date "
                   + "FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return list;
            }

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setType(rs.getString("type"));
                    t.setCategory(rs.getString("category"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setDescription(rs.getString("description"));

                    String dateVal = rs.getString("transaction_date");
                    if (dateVal != null) {
                        try {
                            t.setTransactionDate(Date.valueOf(dateVal));
                        } catch (Exception e) {
                            // Fallback if timestamp format
                            t.setTransactionDate(new Date(System.currentTimeMillis()));
                        }
                    }
                    list.add(t);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // Secure Update Transaction enforcing user ownership
    public boolean updateTransaction(Transaction transaction, int userId) {
        if (transaction == null) {
            return false;
        }

        String sql = "UPDATE transactions SET type = ?, category = ?, amount = ?, description = ?, transaction_date = ? "
                   + "WHERE id = ? AND user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            ps.setString(1, transaction.getType());
            ps.setString(2, transaction.getCategory());
            ps.setDouble(3, transaction.getAmount());
            ps.setString(4, transaction.getDescription());

            String dateStr = transaction.getTransactionDate() != null
                    ? transaction.getTransactionDate().toString()
                    : new Date(System.currentTimeMillis()).toString();
            ps.setString(5, dateStr);

            ps.setInt(6, transaction.getId());
            ps.setInt(7, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Backward-compatible updateTransaction overload
    public boolean updateTransaction(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        return updateTransaction(transaction, transaction.getUserId());
    }

    // Secure Delete Transaction enforcing user ownership
    public boolean deleteTransaction(int id, int userId) {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            ps.setInt(1, id);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Backward-compatible deleteTransaction
    public boolean deleteTransaction(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Secure Get Transaction by ID with user ownership check
    public Transaction getTransactionById(int id, int userId) {
        Transaction transaction = null;
        String sql = "SELECT id, user_id, type, category, amount, description, transaction_date "
                   + "FROM transactions WHERE id = ? AND user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return null;
            }

            ps.setInt(1, id);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    transaction = new Transaction();
                    transaction.setId(rs.getInt("id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setType(rs.getString("type"));
                    transaction.setCategory(rs.getString("category"));
                    transaction.setAmount(rs.getDouble("amount"));
                    transaction.setDescription(rs.getString("description"));

                    String dateVal = rs.getString("transaction_date");
                    if (dateVal != null) {
                        try {
                            transaction.setTransactionDate(Date.valueOf(dateVal));
                        } catch (Exception e) {
                            transaction.setTransactionDate(new Date(System.currentTimeMillis()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return transaction;
    }

    // Backward-compatible getTransactionById
    public Transaction getTransactionById(int id) {
        Transaction transaction = null;
        String sql = "SELECT id, user_id, type, category, amount, description, transaction_date "
                   + "FROM transactions WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return null;
            }

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    transaction = new Transaction();
                    transaction.setId(rs.getInt("id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setType(rs.getString("type"));
                    transaction.setCategory(rs.getString("category"));
                    transaction.setAmount(rs.getDouble("amount"));
                    transaction.setDescription(rs.getString("description"));

                    String dateVal = rs.getString("transaction_date");
                    if (dateVal != null) {
                        try {
                            transaction.setTransactionDate(Date.valueOf(dateVal));
                        } catch (Exception e) {
                            transaction.setTransactionDate(new Date(System.currentTimeMillis()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return transaction;
    }

    // Total Income
    public double getTotalIncome(int userId) {
        double total = 0.0;
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE user_id = ? AND type = 'Income'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return 0.0;
            }

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return total;
    }

    // Total Expense
    public double getTotalExpense(int userId) {
        double total = 0.0;
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE user_id = ? AND type = 'Expense'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return 0.0;
            }

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return total;
    }

    // Raw summary container for database aggregation results
    public static class SummaryRecord {
        private final double totalIncome;
        private final double totalExpense;
        private final int count;

        public SummaryRecord(double totalIncome, double totalExpense, int count) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.count = count;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public int getCount() {
            return count;
        }
    }

    // Filtered Financial Summary Aggregation Query (Inclusive dates)
    public SummaryRecord getSummaryByDateRange(int userId, String startDate, String endDate) {
        String sql = "SELECT "
                   + "COALESCE(SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END), 0.0) AS income, "
                   + "COALESCE(SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END), 0.0) AS expense, "
                   + "COUNT(id) AS tx_count "
                   + "FROM transactions "
                   + "WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return new SummaryRecord(0.0, 0.0, 0);
            }

            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SummaryRecord(rs.getDouble("income"), rs.getDouble("expense"), rs.getInt("tx_count"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new SummaryRecord(0.0, 0.0, 0);
    }

    // Record holding aggregated monthly values
    public static class MonthlyRecord {
        private final String yearMonth; // e.g. "2026-11"
        private final double income;
        private final double expense;

        public MonthlyRecord(String yearMonth, double income, double expense) {
            this.yearMonth = yearMonth;
            this.income = income;
            this.expense = expense;
        }

        public String getYearMonth() {
            return yearMonth;
        }

        public double getIncome() {
            return income;
        }

        public double getExpense() {
            return expense;
        }
    }

    // Monthly income and expense totals aggregated via SQLite strftime
    public List<MonthlyRecord> getMonthlyIncomeAndExpenses(int userId, String startDate, String endDate) {
        List<MonthlyRecord> results = new ArrayList<>();
        String sql = "SELECT "
                   + "strftime('%Y-%m', transaction_date) AS ym, "
                   + "COALESCE(SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END), 0.0) AS total_income, "
                   + "COALESCE(SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END), 0.0) AS total_expense "
                   + "FROM transactions "
                   + "WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ? "
                   + "GROUP BY ym "
                   + "ORDER BY ym ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return results;
            }

            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ym = rs.getString("ym");
                    if (ym != null) {
                        results.add(new MonthlyRecord(
                                ym,
                                rs.getDouble("total_income"),
                                rs.getDouble("total_expense")
                        ));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    // Find the earliest transaction date for a user
    public String getEarliestTransactionDate(int userId) {
        String sql = "SELECT MIN(transaction_date) FROM transactions WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return null;
            }

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
