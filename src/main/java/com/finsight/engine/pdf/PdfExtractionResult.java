package com.finsight.engine.pdf;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result model representing the outcome of secure PDF text extraction.
 * Decouples low-level document text extraction from downstream transaction parsing.
 */
public class PdfExtractionResult {

    public enum Status {
        SUCCESS,
        SUCCESS_TRUNCATED,
        EMPTY_INPUT,
        OVERSIZED_INPUT,
        INVALID_SIGNATURE,
        PASSWORD_PROTECTED,
        INSUFFICIENT_DIGITAL_TEXT,
        SCANNED_OR_EMPTY_TEXT, // Retained for backward-compatibility
        EXTRACTION_FAILURE
    }

    private final Status status;
    private final boolean success;
    private final String text;
    private final int pageCount;
    private final int characterCount;
    private final String errorMessage;
    private final List<String> warnings;
    private final boolean isTruncated;

    public PdfExtractionResult(Status status,
                               String text,
                               int pageCount,
                               int characterCount,
                               String errorMessage,
                               List<String> warnings,
                               boolean isTruncated) {
        this.status = status;
        this.success = (status == Status.SUCCESS || status == Status.SUCCESS_TRUNCATED);
        this.text = text != null ? text : "";
        this.pageCount = pageCount;
        this.characterCount = characterCount;
        this.errorMessage = errorMessage;
        this.warnings = (warnings != null) ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.isTruncated = isTruncated;
    }

    public static PdfExtractionResult success(String text, int pageCount, List<String> warnings) {
        String cleanText = (text != null) ? text : "";
        return new PdfExtractionResult(
                Status.SUCCESS,
                cleanText,
                pageCount,
                cleanText.length(),
                null,
                warnings,
                false
        );
    }

    public static PdfExtractionResult successTruncated(String text, int pageCount, List<String> warnings) {
        String cleanText = (text != null) ? text : "";
        return new PdfExtractionResult(
                Status.SUCCESS_TRUNCATED,
                cleanText,
                pageCount,
                cleanText.length(),
                null,
                warnings,
                true
        );
    }

    public static PdfExtractionResult emptyInput() {
        return failure(Status.EMPTY_INPUT, "Uploaded PDF file is empty or null.", 0);
    }

    public static PdfExtractionResult failure(Status status, String errorMessage, int pageCount) {
        return new PdfExtractionResult(
                status,
                "",
                pageCount,
                0,
                errorMessage,
                Collections.emptyList(),
                false
        );
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getText() {
        return text;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    @Override
    public String toString() {
        return "PdfExtractionResult{" +
                "status=" + status +
                ", success=" + success +
                ", pageCount=" + pageCount +
                ", charCount=" + characterCount +
                ", isTruncated=" + isTruncated +
                ", error='" + errorMessage + '\'' +
                ", warnings=" + warnings.size() +
                '}';
    }
}
