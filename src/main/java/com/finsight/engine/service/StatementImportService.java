package com.finsight.engine.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.staging.StagingSession;
import com.finsight.engine.staging.StagingSession.ImportStatus;
import com.finsight.engine.staging.StagingSession.StagedTransactionItem;

/**
 * Service orchestrating secure atomic import of staged transactions into the database.
 * Enforces user ownership, prevents duplicate imports, and preserves refund metadata.
 */
public class StatementImportService {

    private final TransactionDAO transactionDAO;

    public StatementImportService() {
        this(new TransactionDAO());
    }

    public StatementImportService(TransactionDAO transactionDAO) {
        this.transactionDAO = Objects.requireNonNull(transactionDAO, "transactionDAO must not be null");
    }

    public static class ImportResult {
        private final boolean success;
        private final int importedCount;
        private final int skippedCount;
        private final String message;
        private final List<String> warnings;

        public ImportResult(boolean success, int importedCount, int skippedCount, String message, List<String> warnings) {
            this.success = success;
            this.importedCount = importedCount;
            this.skippedCount = skippedCount;
            this.message = message;
            this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        }

        public boolean isSuccess() {
            return success;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    /**
     * Executes atomic import of approved staged transactions.
     */
    public ImportResult importStagedTransactions(StagingSession session, int userId) {
        if (session == null) {
            return new ImportResult(false, 0, 0, "Staging session not found or expired.", Collections.emptyList());
        }

        if (session.getUserId() != userId) {
            return new ImportResult(false, 0, 0, "Unauthorized: Staging session does not belong to user.", Collections.emptyList());
        }

        if (session.isExpired()) {
            return new ImportResult(false, 0, 0, "Staging session has expired. Please re-upload your statement.", Collections.emptyList());
        }

        // Atomically claim import ownership (PENDING_REVIEW -> IMPORTING)
        boolean claimed = session.claimForImport();
        if (!claimed) {
            if (session.getImportStatus() == ImportStatus.IMPORTED) {
                return new ImportResult(false, 0, 0, "This statement has already been imported.", Collections.emptyList());
            } else if (session.getImportStatus() == ImportStatus.IMPORTING) {
                return new ImportResult(false, 0, 0, "Import is currently in progress by another request.", Collections.emptyList());
            } else {
                return new ImportResult(false, 0, 0, "Staging session cannot be imported in its current state.", Collections.emptyList());
            }
        }

        boolean commitSucceeded = false;
        try {
            List<StagedTransactionItem> items = session.getItemsList();
            List<Transaction> toImport = new ArrayList<>();
            int skippedCount = 0;
            List<String> warnings = new ArrayList<>();

            // Retrieve existing transactions to prevent double-inserting identical records
            List<Transaction> existingTxs = transactionDAO.getTransactionsByUser(userId);
            Set<String> existingSignatures = new HashSet<>();
            if (existingTxs != null) {
                for (Transaction et : existingTxs) {
                    String sig = buildSignature(et.getTransactionDate() != null ? et.getTransactionDate().toLocalDate() : null,
                            BigDecimal.valueOf(et.getAmount()),
                            et.getDescription());
                    existingSignatures.add(sig);
                }
            }

            for (StagedTransactionItem item : items) {
                if (!item.isSelected()) {
                    skippedCount++;
                    continue;
                }

                ParsedTransaction pt = item.getTransaction();
                if (pt.isConfirmedDuplicate()) {
                    skippedCount++;
                    warnings.add("Skipped confirmed duplicate transaction: " + pt.getDescription());
                    continue;
                }

                // Transfers handling: Do not count transfers as ordinary income or expenses
                if (pt.getTransactionType() == TransactionType.TRANSFER) {
                    skippedCount++;
                    warnings.add("Excluded account transfer from income/expense import: " + pt.getDescription());
                    continue;
                }

                BigDecimal amt = pt.getAmount();
                if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                    skippedCount++;
                    warnings.add("Skipped transaction with zero or negative amount: " + pt.getDescription());
                    continue;
                }

                LocalDate txDate = pt.getTransactionDate() != null ? pt.getTransactionDate() : LocalDate.now();

                // Duplicate prevention check against existing DB
                String currentSig = buildSignature(txDate, amt, pt.getDescription());
                if (existingSignatures.contains(currentSig)) {
                    skippedCount++;
                    warnings.add("Skipped already existing transaction in database: " + pt.getDescription());
                    continue;
                }

                Transaction dbTx = new Transaction();
                dbTx.setUserId(userId);
                dbTx.setTransactionDate(Date.valueOf(txDate));
                dbTx.setAmount(amt.doubleValue());
                dbTx.setCategory(pt.getCategory() != null ? pt.getCategory() : "Other");

                // Format type: Income vs Expense
                if (pt.getTransactionType() == TransactionType.INCOME) {
                    dbTx.setType("Income");
                    if (pt.isRefundOrReversal()) {
                        // Tag description with [Refund] for downstream accounting traceability
                        String desc = pt.getDescription() != null ? pt.getDescription() : "";
                        if (!desc.toUpperCase().contains("REFUND")) {
                            dbTx.setDescription("[Refund] " + desc);
                        } else {
                            dbTx.setDescription(desc);
                        }
                    } else {
                        dbTx.setDescription(pt.getDescription() != null ? pt.getDescription() : "");
                    }
                } else {
                    dbTx.setType("Expense");
                    dbTx.setDescription(pt.getDescription() != null ? pt.getDescription() : "");
                }

                toImport.add(dbTx);
                existingSignatures.add(currentSig);
            }

            if (toImport.isEmpty()) {
                commitSucceeded = true;
                session.setImportStatus(ImportStatus.IMPORTED);
                return new ImportResult(true, 0, skippedCount, "No transactions were selected or eligible for import.", warnings);
            }

            boolean success = transactionDAO.batchImportTransactions(userId, toImport);
            if (success) {
                commitSucceeded = true;
                session.setImportStatus(ImportStatus.IMPORTED);
                return new ImportResult(true, toImport.size(), skippedCount,
                        "Successfully imported " + toImport.size() + " transactions.", warnings);
            } else {
                return new ImportResult(false, 0, skippedCount,
                        "Database transaction failed. All changes have been rolled back.", warnings);
            }
        } finally {
            if (!commitSucceeded) {
                // Safely release claim back to PENDING_REVIEW for retry
                session.releaseImportClaim();
            }
        }
    }

    private String buildSignature(LocalDate date, BigDecimal amount, String desc) {
        String dStr = date != null ? date.toString() : "";
        String aStr = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP).toString() : "0.00";
        String descStr = desc != null ? desc.trim().toLowerCase() : "";
        return dStr + "|" + aStr + "|" + descStr;
    }
}

