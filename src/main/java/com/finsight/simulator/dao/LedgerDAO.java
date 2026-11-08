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
import com.finsight.simulator.model.LedgerEntry;
import com.finsight.simulator.model.LedgerEntry.EntryType;
import com.finsight.simulator.model.SimulatorTransaction;
import com.finsight.simulator.model.SimulatorTransaction.TransactionStatus;
import com.finsight.simulator.model.SimulatorTransaction.TransactionType;

public class LedgerDAO {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountDAO accountDAO = new AccountDAO();

    public static class TransactionExecutionResult {
        private final boolean success;
        private final String message;
        private final SimulatorTransaction transaction;
        private final BigDecimal updatedBalance;

        public TransactionExecutionResult(boolean success, String message, SimulatorTransaction transaction, BigDecimal updatedBalance) {
            this.success = success;
            this.message = message;
            this.transaction = transaction;
            this.updatedBalance = updatedBalance;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public SimulatorTransaction getTransaction() { return transaction; }
        public BigDecimal getUpdatedBalance() { return updatedBalance; }
    }

    public TransactionExecutionResult executeDeposit(int userId, int accountId, BigDecimal amount, String description, String category, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new TransactionExecutionResult(false, "Deposit amount must be strictly greater than zero.", null, null);
        }

        Account account = accountDAO.getAccountById(accountId);
        if (account == null || account.getUserId() != userId) {
            return new TransactionExecutionResult(false, "Account not found or access denied.", null, null);
        }

        if (!account.isActive()) {
            return new TransactionExecutionResult(false, "Account is not active.", null, null);
        }

        // Check idempotency with payload validation
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            SimulatorTransaction existing = getTransactionByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                if (existing.getTransactionType() != TransactionType.DEPOSIT
                        || existing.getAmount().compareTo(amount) != 0
                        || (existing.getDestinationAccountId() != null && existing.getDestinationAccountId() != accountId)) {
                    return new TransactionExecutionResult(false, "Conflict: Idempotency key already used with differing transaction payload.", null, null);
                }
                return new TransactionExecutionResult(true, "Transaction already processed.", existing, account.getBalance());
            }
        }

        String ref = generateReferenceNumber("DEP");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return new TransactionExecutionResult(false, "Database connection unavailable.", null, null);
            con.setAutoCommit(false);

            try {
                Account clearingAcc = accountDAO.getOrCreateClearingAccount(con);

                // 1. Insert Simulator Transaction (Source: Clearing, Destination: Customer Account)
                int txId = insertTransaction(con, ref, TransactionType.DEPOSIT, amount, description, category,
                        TransactionStatus.COMPLETED, clearingAcc.getId(), accountId, idempotencyKey);

                // 2. Insert Balanced Double-Entry Ledger Entries (Debit Clearing, Credit Customer Account)
                insertLedgerEntry(con, txId, clearingAcc.getId(), EntryType.DEBIT, amount);
                insertLedgerEntry(con, txId, accountId, EntryType.CREDIT, amount);

                // 3. Atomically update account balance
                BigDecimal newBalance = account.getBalance().add(amount);
                accountDAO.updateBalance(con, accountId, newBalance);

                con.commit();
                SimulatorTransaction tx = getTransactionByReference(ref);
                return new TransactionExecutionResult(true, "Deposit successful.", tx, newBalance);

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return new TransactionExecutionResult(false, "Deposit failed: " + e.getMessage(), null, null);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new TransactionExecutionResult(false, "Database error: " + e.getMessage(), null, null);
        }
    }

    public TransactionExecutionResult executeWithdrawal(int userId, int accountId, BigDecimal amount, String description, String category, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new TransactionExecutionResult(false, "Withdrawal amount must be strictly greater than zero.", null, null);
        }

        Account account = accountDAO.getAccountById(accountId);
        if (account == null || account.getUserId() != userId) {
            return new TransactionExecutionResult(false, "Account not found or access denied.", null, null);
        }

        if (!account.isActive()) {
            return new TransactionExecutionResult(false, "Account is not active.", null, null);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return new TransactionExecutionResult(false, "Insufficient available funds in account.", null, null);
        }

        // Check idempotency with payload validation
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            SimulatorTransaction existing = getTransactionByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                if (existing.getTransactionType() != TransactionType.WITHDRAWAL
                        || existing.getAmount().compareTo(amount) != 0
                        || (existing.getSourceAccountId() != null && existing.getSourceAccountId() != accountId)) {
                    return new TransactionExecutionResult(false, "Conflict: Idempotency key already used with differing transaction payload.", null, null);
                }
                return new TransactionExecutionResult(true, "Transaction already processed.", existing, account.getBalance());
            }
        }

        String ref = generateReferenceNumber("WTH");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return new TransactionExecutionResult(false, "Database connection unavailable.", null, null);
            con.setAutoCommit(false);

            try {
                // Re-verify balance within transaction
                BigDecimal currentBalance = getCurrentAccountBalance(con, accountId);
                if (currentBalance.compareTo(amount) < 0) {
                    con.rollback();
                    return new TransactionExecutionResult(false, "Insufficient balance.", null, null);
                }

                Account clearingAcc = accountDAO.getOrCreateClearingAccount(con);

                // 1. Insert Simulator Transaction (Source: Customer Account, Destination: Clearing)
                int txId = insertTransaction(con, ref, TransactionType.WITHDRAWAL, amount, description, category,
                        TransactionStatus.COMPLETED, accountId, clearingAcc.getId(), idempotencyKey);

                // 2. Insert Balanced Double-Entry Ledger Entries (Debit Customer Account, Credit Clearing)
                insertLedgerEntry(con, txId, accountId, EntryType.DEBIT, amount);
                insertLedgerEntry(con, txId, clearingAcc.getId(), EntryType.CREDIT, amount);

                // 3. Atomically update account balance
                BigDecimal newBalance = currentBalance.subtract(amount);
                accountDAO.updateBalance(con, accountId, newBalance);

                con.commit();
                SimulatorTransaction tx = getTransactionByReference(ref);
                return new TransactionExecutionResult(true, "Withdrawal successful.", tx, newBalance);

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return new TransactionExecutionResult(false, "Withdrawal failed: " + e.getMessage(), null, null);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new TransactionExecutionResult(false, "Database error: " + e.getMessage(), null, null);
        }
    }

    public TransactionExecutionResult executeTransfer(int senderUserId, int sourceAccountId, String recipientAccountNumber,
                                                    BigDecimal amount, String description, String category, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new TransactionExecutionResult(false, "Transfer amount must be strictly greater than zero.", null, null);
        }

        Account sourceAccount = accountDAO.getAccountById(sourceAccountId);
        if (sourceAccount == null || sourceAccount.getUserId() != senderUserId) {
            return new TransactionExecutionResult(false, "Source account not found or access denied.", null, null);
        }

        if (!sourceAccount.isActive()) {
            return new TransactionExecutionResult(false, "Source account is not active.", null, null);
        }

        Account recipientAccount = accountDAO.getAccountByNumber(recipientAccountNumber);
        if (recipientAccount == null) {
            return new TransactionExecutionResult(false, "Recipient account not found.", null, null);
        }

        if (!recipientAccount.isActive()) {
            return new TransactionExecutionResult(false, "Recipient account is inactive or blocked.", null, null);
        }

        if (sourceAccount.getId() == recipientAccount.getId()) {
            return new TransactionExecutionResult(false, "Source and destination accounts must be distinct.", null, null);
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            return new TransactionExecutionResult(false, "Insufficient funds for transfer.", null, null);
        }

        // Idempotency check with payload validation
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            SimulatorTransaction existing = getTransactionByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                if (existing.getTransactionType() != TransactionType.TRANSFER
                        || existing.getAmount().compareTo(amount) != 0
                        || (existing.getSourceAccountId() != null && existing.getSourceAccountId() != sourceAccountId)
                        || (existing.getDestinationAccountId() != null && existing.getDestinationAccountId() != recipientAccount.getId())) {
                    return new TransactionExecutionResult(false, "Conflict: Idempotency key already used with differing transaction payload.", null, null);
                }
                return new TransactionExecutionResult(true, "Transfer already processed.", existing, sourceAccount.getBalance());
            }
        }

        String ref = generateReferenceNumber("TRF");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return new TransactionExecutionResult(false, "Database connection unavailable.", null, null);
            con.setAutoCommit(false);

            try {
                // Re-verify sender balance inside transaction
                BigDecimal currentSenderBalance = getCurrentAccountBalance(con, sourceAccountId);
                if (currentSenderBalance.compareTo(amount) < 0) {
                    con.rollback();
                    return new TransactionExecutionResult(false, "Insufficient balance.", null, null);
                }

                BigDecimal currentRecipientBalance = getCurrentAccountBalance(con, recipientAccount.getId());

                // 1. Insert High-level Simulator Transaction
                int txId = insertTransaction(con, ref, TransactionType.TRANSFER, amount, description, category,
                        TransactionStatus.COMPLETED, sourceAccountId, recipientAccount.getId(), idempotencyKey);

                // 2. Insert Balanced Double-Entry Ledger Entries (Total Debits == Total Credits)
                insertLedgerEntry(con, txId, sourceAccountId, EntryType.DEBIT, amount);
                insertLedgerEntry(con, txId, recipientAccount.getId(), EntryType.CREDIT, amount);

                // 3. Atomically update both account balances
                BigDecimal newSenderBalance = currentSenderBalance.subtract(amount);
                BigDecimal newRecipientBalance = currentRecipientBalance.add(amount);

                accountDAO.updateBalance(con, sourceAccountId, newSenderBalance);
                accountDAO.updateBalance(con, recipientAccount.getId(), newRecipientBalance);

                con.commit();
                SimulatorTransaction tx = getTransactionByReference(ref);
                return new TransactionExecutionResult(true, "Transfer of ₹" + amount.toPlainString() + " to " + recipientAccountNumber + " completed successfully.", tx, newSenderBalance);

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return new TransactionExecutionResult(false, "Transfer failed: " + e.getMessage(), null, null);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new TransactionExecutionResult(false, "Database error: " + e.getMessage(), null, null);
        }
    }

    public List<SimulatorTransaction> getTransactionsForAccount(int accountId, int limit) {
        List<SimulatorTransaction> list = new ArrayList<>();
        String sql = "SELECT id, reference_number, transaction_type, amount, description, category, status, "
                + "source_account_id, destination_account_id, idempotency_key, created_at "
                + "FROM simulator_transactions "
                + "WHERE source_account_id = ? OR destination_account_id = ? "
                + "ORDER BY id DESC LIMIT ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            ps.setInt(3, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SimulatorTransaction> getTransactionsForUser(int userId, int limit) {
        List<SimulatorTransaction> list = new ArrayList<>();
        String sql = "SELECT DISTINCT st.id, st.reference_number, st.transaction_type, st.amount, st.description, st.category, st.status, "
                + "st.source_account_id, st.destination_account_id, st.idempotency_key, st.created_at "
                + "FROM simulator_transactions st "
                + "LEFT JOIN accounts src ON st.source_account_id = src.id "
                + "LEFT JOIN accounts dst ON st.destination_account_id = dst.id "
                + "WHERE src.user_id = ? OR dst.user_id = ? "
                + "ORDER BY st.id DESC LIMIT ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public SimulatorTransaction getTransactionByReference(String referenceNumber) {
        String sql = "SELECT id, reference_number, transaction_type, amount, description, category, status, "
                + "source_account_id, destination_account_id, idempotency_key, created_at "
                + "FROM simulator_transactions WHERE reference_number = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setString(1, referenceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTransaction(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public SimulatorTransaction getTransactionByIdempotencyKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        String sql = "SELECT id, reference_number, transaction_type, amount, description, category, status, "
                + "source_account_id, destination_account_id, idempotency_key, created_at "
                + "FROM simulator_transactions WHERE idempotency_key = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTransaction(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigDecimal calculateReconciledBalance(int accountId) {
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END), 0) - "
                + "COALESCE(SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END), 0) "
                + "FROM ledger_entries WHERE account_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return BigDecimal.ZERO;
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return BigDecimal.valueOf(rs.getDouble(1)).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getCurrentAccountBalance(Connection con, int accountId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return BigDecimal.valueOf(rs.getDouble(1)).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }
        }
        throw new SQLException("Account not found: " + accountId);
    }

    private int insertTransaction(Connection con, String ref, TransactionType type, BigDecimal amount, String desc,
                                  String category, TransactionStatus status, Integer srcId, Integer dstId, String idempKey) throws SQLException {
        String sql = "INSERT INTO simulator_transactions (reference_number, transaction_type, amount, description, category, status, "
                + "source_account_id, destination_account_id, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ref);
            ps.setString(2, type.name());
            ps.setDouble(3, amount.doubleValue());
            ps.setString(4, desc);
            ps.setString(5, category != null ? category : "Other");
            ps.setString(6, status.name());
            if (srcId != null) ps.setInt(7, srcId); else ps.setNull(7, java.sql.Types.INTEGER);
            if (dstId != null) ps.setInt(8, dstId); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.setString(9, idempKey);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated transaction id.");
    }

    private void insertLedgerEntry(Connection con, int txId, int accountId, EntryType entryType, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, txId);
            ps.setInt(2, accountId);
            ps.setString(3, entryType.name());
            ps.setDouble(4, amount.doubleValue());
            ps.executeUpdate();
        }
    }

    private String generateReferenceNumber(String prefix) {
        long code = 100000000000L + (long)(RANDOM.nextDouble() * 900000000000L);
        return prefix + "-" + code;
    }

    private SimulatorTransaction mapTransaction(ResultSet rs) throws Exception {
        SimulatorTransaction tx = new SimulatorTransaction();
        tx.setId(rs.getInt("id"));
        tx.setReferenceNumber(rs.getString("reference_number"));
        tx.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        tx.setAmount(BigDecimal.valueOf(rs.getDouble("amount")).setScale(2, java.math.RoundingMode.HALF_UP));
        tx.setDescription(rs.getString("description"));
        tx.setCategory(rs.getString("category"));
        tx.setStatus(TransactionStatus.valueOf(rs.getString("status")));

        int src = rs.getInt("source_account_id");
        tx.setSourceAccountId(rs.wasNull() ? null : src);

        int dst = rs.getInt("destination_account_id");
        tx.setDestinationAccountId(rs.wasNull() ? null : dst);

        tx.setIdempotencyKey(rs.getString("idempotency_key"));
        tx.setCreatedAt(rs.getString("created_at"));
        return tx;
    }

    public List<LedgerEntry> getLedgerEntriesForTransaction(int transactionId) {
        List<LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT id, transaction_id, account_id, entry_type, amount, created_at FROM ledger_entries WHERE transaction_id = ? ORDER BY id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return list;
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LedgerEntry entry = new LedgerEntry();
                    entry.setId(rs.getInt("id"));
                    entry.setTransactionId(rs.getInt("transaction_id"));
                    entry.setAccountId(rs.getInt("account_id"));
                    entry.setEntryType(EntryType.valueOf(rs.getString("entry_type")));
                    entry.setAmount(BigDecimal.valueOf(rs.getDouble("amount")).setScale(2, java.math.RoundingMode.HALF_UP));
                    entry.setCreatedAt(rs.getString("created_at"));
                    list.add(entry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

