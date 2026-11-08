package com.finsight.engine.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Dedicated, secure PDF extraction engine using Apache PDFBox 3.x.
 * 
 * Security & Reliability Policies:
 * 1. Controlled Byte Streams: Validates input length without unbounded memory growth.
 * 2. Magic Bytes Validation: Verifies %PDF- signature before invoking parser.
 * 3. Configurable Maximum Upload Size: Defaults to 10 MB.
 * 4. Password Protection Detection: Safely identifies and rejects encrypted PDFs with actionable guidance.
 * 5. Bounded Text Extraction (SEC-01): Enforces maxExtractedChars directly during stripper character writing
 *    using a custom BoundedStringWriter. Extraction stops immediately without accumulating unbounded text in memory.
 * 6. Non-Destructive Raw Text Handling (SEC-02): Raw statement narrations, UTRs, and monetary sequences
 *    are preserved without destructive context-blind regex substitution.
 * 7. Accurate Status & Minimum Text Policy (SEC-04): Uses neutral "no extractable digital text layer detected"
 *    rather than assuming an image scan.
 * 8. Zero Leakage: Strips stack traces and internal paths from user error messages.
 * 9. Resource Lifecycle: All documents and streams closed reliably via try-with-resources.
 */
public class SecurePdfExtractor {

    public static final int DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final int DEFAULT_MIN_TEXT_CHARS = 40;              // Minimum threshold to consider digital text present
    public static final int DEFAULT_MAX_EXTRACTED_CHARS = 2_000_000;   // 2M chars safety limit

    private final int maxFileSize;
    private final int minTextChars;
    private final int maxExtractedChars;

    public SecurePdfExtractor() {
        this(DEFAULT_MAX_FILE_SIZE, DEFAULT_MIN_TEXT_CHARS, DEFAULT_MAX_EXTRACTED_CHARS);
    }

    public SecurePdfExtractor(int maxFileSize, int minTextChars, int maxExtractedChars) {
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.minTextChars = minTextChars > 0 ? minTextChars : DEFAULT_MIN_TEXT_CHARS;
        this.maxExtractedChars = maxExtractedChars > 0 ? maxExtractedChars : DEFAULT_MAX_EXTRACTED_CHARS;
    }

    /**
     * Extracts digital text from a byte array with streaming character bound enforcement.
     */
    public PdfExtractionResult extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.EMPTY_INPUT,
                    "Uploaded PDF file is empty.",
                    0
            );
        }

        if (pdfBytes.length > maxFileSize) {
            long maxMb = maxFileSize / (1024 * 1024);
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.OVERSIZED_INPUT,
                    "Uploaded file exceeds the maximum allowed size of " + maxMb + " MB.",
                    0
            );
        }

        // Validate %PDF- signature
        if (!hasPdfSignature(pdfBytes)) {
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.INVALID_SIGNATURE,
                    "Invalid file format: File header does not match standard PDF signature (%PDF-).",
                    0
            );
        }

        List<String> warnings = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();

            if (document.isEncrypted()) {
                return PdfExtractionResult.failure(
                        PdfExtractionResult.Status.PASSWORD_PROTECTED,
                        "The uploaded PDF is password-protected. Please upload an unprotected copy of your e-statement.",
                        pageCount
                );
            }

            AccessPermission permissions = document.getCurrentAccessPermission();
            if (permissions != null && !permissions.canExtractContent()) {
                return PdfExtractionResult.failure(
                        PdfExtractionResult.Status.PASSWORD_PROTECTED,
                        "The uploaded PDF restricts content extraction. Please provide an e-statement with content copying enabled.",
                        pageCount
                );
            }

            // Stream extraction into bounded writer to prevent unbounded heap accumulation
            BoundedStringWriter boundedWriter = new BoundedStringWriter(maxExtractedChars);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            boolean reachedLimit = false;
            try {
                stripper.writeText(document, boundedWriter);
            } catch (CharacterLimitExceededException e) {
                reachedLimit = true;
                warnings.add("Extracted text reached the safety limit of " + maxExtractedChars 
                        + " characters and was safely bounded during extraction.");
            }

            String extractedText = boundedWriter.toString();

            if (extractedText == null || extractedText.trim().length() < minTextChars) {
                return PdfExtractionResult.failure(
                        PdfExtractionResult.Status.INSUFFICIENT_DIGITAL_TEXT,
                        "No readable digital text layer was detected (less than " + minTextChars + " characters found). " +
                        "FinSight requires a digitally generated bank statement with extractable text.",
                        pageCount
                );
            }

            if (reachedLimit) {
                return PdfExtractionResult.successTruncated(extractedText, pageCount, warnings);
            }

            return PdfExtractionResult.success(extractedText, pageCount, warnings);

        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.PASSWORD_PROTECTED,
                    "The uploaded PDF is password-protected. Please upload an unprotected copy of your e-statement.",
                    0
            );
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("encrypted")) {
                return PdfExtractionResult.failure(
                        PdfExtractionResult.Status.PASSWORD_PROTECTED,
                        "The uploaded PDF is password-protected. Please upload an unprotected copy of your e-statement.",
                        0
                );
            }
            String safeMsg = e.getMessage() != null && !e.getMessage().contains(":") 
                    ? e.getMessage() 
                    : "The document appears to be corrupted or not conforming to PDF specifications.";
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.EXTRACTION_FAILURE,
                    "Failed to parse PDF document: " + safeMsg,
                    0
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("encrypted")) {
                return PdfExtractionResult.failure(
                        PdfExtractionResult.Status.PASSWORD_PROTECTED,
                        "The uploaded PDF is password-protected. Please upload an unprotected copy of your e-statement.",
                        0
                );
            }
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.EXTRACTION_FAILURE,
                    "An unexpected error occurred while processing the PDF.",
                    0
            );
        }
    }

    /**
     * Safely reads an InputStream up to maxFileSize, then processes.
     * Stops reading immediately if the incoming stream exceeds maxFileSize without buffering.
     */
    public PdfExtractionResult extract(InputStream inputStream) {
        if (inputStream == null) {
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.EMPTY_INPUT,
                    "Input stream is null.",
                    0
            );
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int totalBytes = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(chunk)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxFileSize) {
                    long maxMb = maxFileSize / (1024 * 1024);
                    return PdfExtractionResult.failure(
                            PdfExtractionResult.Status.OVERSIZED_INPUT,
                            "Uploaded file exceeds the maximum allowed size of " + maxMb + " MB.",
                            0
                    );
                }
                buffer.write(chunk, 0, bytesRead);
            }

            return extract(buffer.toByteArray());

        } catch (IOException e) {
            return PdfExtractionResult.failure(
                    PdfExtractionResult.Status.EXTRACTION_FAILURE,
                    "Error reading uploaded PDF stream: " + e.getMessage(),
                    0
            );
        }
    }

    /**
     * Checks first 5 bytes for standard PDF signature (%PDF-).
     */
    public static boolean hasPdfSignature(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            return false;
        }
        return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-';
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public int getMinTextChars() {
        return minTextChars;
    }

    public int getMaxExtractedChars() {
        return maxExtractedChars;
    }

    /**
     * Custom Writer that enforces a hard character ceiling during PDFBox text stripping.
     * Throws CharacterLimitExceededException as soon as the ceiling is reached to abort
     * further extraction and avoid unbounded heap growth.
     */
    public static class BoundedStringWriter extends Writer {
        private final StringBuilder sb;
        private final int maxChars;

        public BoundedStringWriter(int maxChars) {
            this.maxChars = maxChars;
            this.sb = new StringBuilder(Math.min(maxChars, 16384));
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (cbuf == null || len <= 0) {
                return;
            }
            int available = maxChars - sb.length();
            if (available <= 0) {
                throw new CharacterLimitExceededException("Extraction character limit reached: " + maxChars);
            }

            if (len <= available) {
                sb.append(cbuf, off, len);
            } else {
                sb.append(cbuf, off, available);
                throw new CharacterLimitExceededException("Extraction character limit reached: " + maxChars);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return sb.toString();
        }

        public int length() {
            return sb.length();
        }
    }

    /**
     * Internal signal exception indicating text limit was reached during writeText.
     */
    public static class CharacterLimitExceededException extends IOException {
        public CharacterLimitExceededException(String message) {
            super(message);
        }
    }
}
