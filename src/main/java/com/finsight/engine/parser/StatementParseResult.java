package com.finsight.engine.parser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.finsight.engine.model.ParsedTransaction;

/**
 * Immutable container encapsulating the complete outcome of a statement parsing operation.
 */
public class StatementParseResult {

    public enum Status {
        SUCCESS,
        PARTIALLY_PARSED,
        UNSUPPORTED_FORMAT,
        AMBIGUOUS_FORMAT,
        PARSING_FAILURE,
        EMPTY_INPUT
    }

    private final Status status;
    private final String bankFormat;
    private final BigDecimal detectionConfidence; // 0.00 to 1.00
    private final List<ParsedTransaction> transactions;
    private final List<String> rawUnparsedLines;
    private final List<String> warnings;
    private final int parsedRowCount;
    private final int rejectedRowCount;
    private final String errorMessage;

    public StatementParseResult(Status status,
                                String bankFormat,
                                BigDecimal detectionConfidence,
                                List<ParsedTransaction> transactions,
                                List<String> rawUnparsedLines,
                                List<String> warnings,
                                int parsedRowCount,
                                int rejectedRowCount,
                                String errorMessage) {
        this.status = status;
        this.bankFormat = bankFormat != null ? bankFormat : "UNKNOWN";
        this.detectionConfidence = detectionConfidence != null ? detectionConfidence : BigDecimal.ZERO;
        this.transactions = transactions != null ? Collections.unmodifiableList(transactions) : Collections.emptyList();
        this.rawUnparsedLines = rawUnparsedLines != null ? Collections.unmodifiableList(rawUnparsedLines) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.parsedRowCount = parsedRowCount;
        this.rejectedRowCount = rejectedRowCount;
        this.errorMessage = errorMessage;
    }

    public static StatementParseResult success(String bankFormat,
                                               BigDecimal detectionConfidence,
                                               List<ParsedTransaction> transactions,
                                               List<String> rawUnparsedLines,
                                               List<String> warnings) {
        int parsed = transactions != null ? transactions.size() : 0;
        int rejected = rawUnparsedLines != null ? rawUnparsedLines.size() : 0;
        Status st = (rejected > 0) ? Status.PARTIALLY_PARSED : Status.SUCCESS;

        return new StatementParseResult(
                st,
                bankFormat,
                detectionConfidence,
                transactions,
                rawUnparsedLines,
                warnings,
                parsed,
                rejected,
                null
        );
    }

    public static StatementParseResult failure(Status status,
                                               String bankFormat,
                                               BigDecimal detectionConfidence,
                                               String errorMessage,
                                               List<String> warnings) {
        return new StatementParseResult(
                status,
                bankFormat,
                detectionConfidence,
                Collections.emptyList(),
                Collections.emptyList(),
                warnings,
                0,
                0,
                errorMessage
        );
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.PARTIALLY_PARSED;
    }

    public String getBankFormat() {
        return bankFormat;
    }

    public BigDecimal getDetectionConfidence() {
        return detectionConfidence;
    }

    public List<ParsedTransaction> getTransactions() {
        return transactions;
    }

    public List<String> getRawUnparsedLines() {
        return rawUnparsedLines;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getParsedRowCount() {
        return parsedRowCount;
    }

    public int getRejectedRowCount() {
        return rejectedRowCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "StatementParseResult{" +
                "status=" + status +
                ", bankFormat='" + bankFormat + '\'' +
                ", confidence=" + detectionConfidence +
                ", parsed=" + parsedRowCount +
                ", rejected=" + rejectedRowCount +
                ", error='" + errorMessage + '\'' +
                '}';
    }
}

