package com.finsight.engine.staging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.finsight.engine.StatementAnalysisResult;
import com.finsight.engine.model.ParsedTransaction;

/**
 * Server-side staging session for uploaded bank statement transactions.
 * Enables users to review, edit, select, and approve transactions before database import.
 */
public class StagingSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ImportStatus {
        PENDING_REVIEW,
        IMPORTING,
        IMPORTED,
        EXPIRED
    }

    private final String stagingId;
    private final int userId;
    private final Instant createdAt;
    private Instant expiresAt;
    private ImportStatus importStatus;

    /**
     * Atomically transitions status from PENDING_REVIEW to IMPORTING.
     * Returns true if successfully claimed, false if already importing, imported, or expired.
     */
    public synchronized boolean claimForImport() {
        if (isExpired()) {
            this.importStatus = ImportStatus.EXPIRED;
            return false;
        }
        if (this.importStatus == ImportStatus.PENDING_REVIEW) {
            this.importStatus = ImportStatus.IMPORTING;
            return true;
        }
        return false;
    }

    /**
     * Releases an in-progress import back to PENDING_REVIEW if safe rollback occurred.
     */
    public synchronized void releaseImportClaim() {
        if (this.importStatus == ImportStatus.IMPORTING) {
            this.importStatus = ImportStatus.PENDING_REVIEW;
        }
    }

    private final String bankName;
    private final String parserFormatId;
    private final BigDecimal detectionConfidence;
    private final StatementAnalysisResult analysisResult;

    // Ordered map of staged transactions: stagingTxId -> StagedTransactionItem
    private final Map<String, StagedTransactionItem> items;

    public static class StagedTransactionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String stagingTxId;
        private boolean selected;
        private ParsedTransaction transaction;

        public StagedTransactionItem(String stagingTxId, boolean selected, ParsedTransaction transaction) {
            this.stagingTxId = stagingTxId;
            this.selected = selected;
            this.transaction = transaction;
        }

        public String getStagingTxId() {
            return stagingTxId;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public ParsedTransaction getTransaction() {
            return transaction;
        }

        public void setTransaction(ParsedTransaction transaction) {
            this.transaction = transaction;
        }
    }

    public StagingSession(int userId, StatementAnalysisResult analysisResult, long ttlSeconds) {
        this.stagingId = UUID.randomUUID().toString();
        this.userId = userId;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(ttlSeconds);
        this.importStatus = ImportStatus.PENDING_REVIEW;
        this.analysisResult = Objects.requireNonNull(analysisResult, "analysisResult must not be null");
        this.bankName = analysisResult.getBankName();
        this.parserFormatId = analysisResult.getParserFormatId();
        this.detectionConfidence = analysisResult.getParserDetectionConfidence();

        this.items = new LinkedHashMap<>();
        if (analysisResult.getFinancialReport() != null && analysisResult.getFinancialReport().getTransactions() != null) {
            int counter = 1;
            for (ParsedTransaction tx : analysisResult.getFinancialReport().getTransactions()) {
                if (isValidStagingTransaction(tx)) {
                    String txId = "stg-" + counter++;
                    boolean defaultSelected = !tx.isConfirmedDuplicate();
                    this.items.put(txId, new StagedTransactionItem(txId, defaultSelected, new ParsedTransaction(tx)));
                }
            }
        }
    }

    private static boolean isValidStagingTransaction(ParsedTransaction tx) {
        if (tx == null) return false;
        if (tx.getTransactionDate() == null) return false;
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (tx.getTransactionType() == null) return false;
        if (tx.getDescription() == null || tx.getDescription().trim().isEmpty()) return false;
        return true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getStagingId() {
        return stagingId;
    }

    public int getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public String getBankName() {
        return bankName;
    }

    public String getParserFormatId() {
        return parserFormatId;
    }

    public BigDecimal getDetectionConfidence() {
        return detectionConfidence;
    }

    public StatementAnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public Map<String, StagedTransactionItem> getItems() {
        return items;
    }

    public List<StagedTransactionItem> getItemsList() {
        return new ArrayList<>(items.values());
    }

    public int getSelectedCount() {
        int count = 0;
        for (StagedTransactionItem item : items.values()) {
            if (item.isSelected()) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return items.size();
    }

    /**
     * Updates an editable field on a staged transaction.
     */
    public synchronized boolean updateTransaction(String stagingTxId,
                                                 LocalDate newDate,
                                                 String newDesc,
                                                 String newMerchant,
                                                 BigDecimal newAmount,
                                                 String newCategory,
                                                 Boolean selected) {
        StagedTransactionItem item = items.get(stagingTxId);
        if (item == null) return false;

        ParsedTransaction tx = item.getTransaction();
        if (newDate != null) tx.setTransactionDate(newDate);
        if (newDesc != null && !newDesc.trim().isEmpty()) tx.setDescription(newDesc.trim());
        if (newMerchant != null) tx.setMerchant(newMerchant.trim());
        if (newAmount != null && newAmount.compareTo(BigDecimal.ZERO) >= 0) {
            tx.setAmount(newAmount);
            if (tx.getTransactionType() == ParsedTransaction.TransactionType.EXPENSE) {
                tx.setDebit(newAmount);
            } else if (tx.getTransactionType() == ParsedTransaction.TransactionType.INCOME) {
                tx.setCredit(newAmount);
            }
        }
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            tx.setCategory(newCategory.trim());
            tx.setManuallyCategorized(true);
            tx.setNeedsUserReview(false);
        }
        if (selected != null) {
            item.setSelected(selected);
        }

        return true;
    }
}

