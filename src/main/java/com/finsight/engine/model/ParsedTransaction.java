package com.finsight.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a parsed transaction candidate extracted from a bank statement.
 * All monetary amounts are stored strictly as BigDecimal to ensure zero floating-point drift.
 */
public class ParsedTransaction {

    public enum TransactionType {
        INCOME,
        EXPENSE,
        TRANSFER
    }

    private String id;
    private LocalDate transactionDate;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal amount;
    private TransactionType transactionType;
    private BigDecimal balance;
    private String merchant;
    private String category;
    private BigDecimal confidenceScore; // 0.00 to 1.00
    private String rawLine;
    private String bankFormat;
    private boolean needsUserReview;
    private boolean duplicateCandidate; // Flagged by detection algorithms as suspected duplicate; requires user review
    private boolean confirmedDuplicate;  // Confirmed as duplicate by system or user; excluded from primary metrics
    private boolean isRefundOrReversal;  // Credit transaction acting as a refund/reversal against expenses
    private boolean manuallyCategorized; // Flag indicating category was explicitly provided or corrected by user

    public ParsedTransaction() {
        this.amount = BigDecimal.ZERO;
        this.confidenceScore = BigDecimal.ONE;
    }

    public ParsedTransaction(String id, LocalDate transactionDate, String description,
                             BigDecimal debit, BigDecimal credit, BigDecimal amount,
                             TransactionType transactionType, BigDecimal balance,
                             String merchant, String category, BigDecimal confidenceScore,
                             String rawLine, String bankFormat, boolean needsUserReview,
                             boolean duplicateCandidate, boolean confirmedDuplicate,
                             boolean isRefundOrReversal) {
        this(id, transactionDate, description, debit, credit, amount, transactionType, balance,
                merchant, category, confidenceScore, rawLine, bankFormat, needsUserReview,
                duplicateCandidate, confirmedDuplicate, isRefundOrReversal, false);
    }

    public ParsedTransaction(String id, LocalDate transactionDate, String description,
                             BigDecimal debit, BigDecimal credit, BigDecimal amount,
                             TransactionType transactionType, BigDecimal balance,
                             String merchant, String category, BigDecimal confidenceScore,
                             String rawLine, String bankFormat, boolean needsUserReview,
                             boolean duplicateCandidate, boolean confirmedDuplicate,
                             boolean isRefundOrReversal, boolean manuallyCategorized) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.description = description;
        this.debit = debit;
        this.credit = credit;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.transactionType = transactionType;
        this.balance = balance;
        this.merchant = merchant;
        this.category = category;
        this.confidenceScore = confidenceScore != null ? confidenceScore : BigDecimal.ONE;
        this.rawLine = rawLine;
        this.bankFormat = bankFormat;
        this.needsUserReview = needsUserReview;
        this.duplicateCandidate = duplicateCandidate;
        this.confirmedDuplicate = confirmedDuplicate;
        this.isRefundOrReversal = isRefundOrReversal;
        this.manuallyCategorized = manuallyCategorized;
    }

    /**
     * Copy constructor for defensive copying.
     */
    public ParsedTransaction(ParsedTransaction other) {
        if (other != null) {
            this.id = other.id;
            this.transactionDate = other.transactionDate;
            this.description = other.description;
            this.debit = other.debit;
            this.credit = other.credit;
            this.amount = other.amount;
            this.transactionType = other.transactionType;
            this.balance = other.balance;
            this.merchant = other.merchant;
            this.category = other.category;
            this.confidenceScore = other.confidenceScore;
            this.rawLine = other.rawLine;
            this.bankFormat = other.bankFormat;
            this.needsUserReview = other.needsUserReview;
            this.duplicateCandidate = other.duplicateCandidate;
            this.confirmedDuplicate = other.confirmedDuplicate;
            this.isRefundOrReversal = other.isRefundOrReversal;
            this.manuallyCategorized = other.manuallyCategorized;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore != null ? confidenceScore : BigDecimal.ONE;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public String getBankFormat() {
        return bankFormat;
    }

    public void setBankFormat(String bankFormat) {
        this.bankFormat = bankFormat;
    }

    public boolean isNeedsUserReview() {
        return needsUserReview;
    }

    public void setNeedsUserReview(boolean needsUserReview) {
        this.needsUserReview = needsUserReview;
    }

    public boolean isDuplicateCandidate() {
        return duplicateCandidate;
    }

    public void setDuplicateCandidate(boolean duplicateCandidate) {
        this.duplicateCandidate = duplicateCandidate;
    }

    public boolean isConfirmedDuplicate() {
        return confirmedDuplicate;
    }

    public void setConfirmedDuplicate(boolean confirmedDuplicate) {
        this.confirmedDuplicate = confirmedDuplicate;
    }

    public boolean isRefundOrReversal() {
        return isRefundOrReversal;
    }

    public void setRefundOrReversal(boolean refundOrReversal) {
        isRefundOrReversal = refundOrReversal;
    }

    public boolean isManuallyCategorized() {
        return manuallyCategorized;
    }

    public void setManuallyCategorized(boolean manuallyCategorized) {
        this.manuallyCategorized = manuallyCategorized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedTransaction that = (ParsedTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ParsedTransaction{" +
                "id='" + id + '\'' +
                ", date=" + transactionDate +
                ", desc='" + description + '\'' +
                ", amount=" + amount +
                ", type=" + transactionType +
                ", category='" + category + '\'' +
                ", manual=" + manuallyCategorized +
                ", candidate=" + duplicateCandidate +
                ", confirmedDuplicate=" + confirmedDuplicate +
                ", isRefund=" + isRefundOrReversal +
                '}';
    }
}
