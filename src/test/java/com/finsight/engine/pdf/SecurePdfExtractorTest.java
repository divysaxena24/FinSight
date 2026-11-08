package com.finsight.engine.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SecurePdfExtractor Step 2A.2 Security Hardening Tests")
public class SecurePdfExtractorTest {

    private SecurePdfExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new SecurePdfExtractor();
    }

    private byte[] createTestPdf(int numPages, String pageTextPrefix) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 1; i <= numPages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (pageTextPrefix != null && !pageTextPrefix.isEmpty()) {
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(font, 12);
                        cs.newLineAtOffset(50, 700);
                        cs.showText(pageTextPrefix + " Page " + i + " Txn: 2026-03-01 GROCERY 1500.00 DEBIT");
                        cs.endText();
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEncryptedPdf(String password, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("1. Normal valid digital PDF extraction")
    void testValidDigitalPdfSinglePage() throws IOException {
        byte[] pdf = createTestPdf(1, "HDFC BANK STATEMENT FOR MARCH 2026.");
        PdfExtractionResult result = extractor.extract(pdf);

        assertTrue(result.isSuccess());
        assertEquals(PdfExtractionResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getPageCount());
        assertTrue(result.getText().contains("HDFC BANK STATEMENT"));
        assertFalse(result.isTruncated());
    }

    @Test
    @DisplayName("2. Large text extraction stops at configured limit and sets SUCCESS_TRUNCATED")
    void testBoundedTextExtractionLimitsHeap() throws IOException {
        // Configure hard ceiling at 80 characters
        SecurePdfExtractor boundedExtractor = new SecurePdfExtractor(10 * 1024 * 1024, 20, 80);
        byte[] pdf = createTestPdf(3, "LONG MULTI-PAGE BANK STATEMENT WITH EXTENSIVE REPEATED FINANCIAL RECORDS");

        PdfExtractionResult result = boundedExtractor.extract(pdf);

        assertTrue(result.isSuccess());
        assertEquals(PdfExtractionResult.Status.SUCCESS_TRUNCATED, result.getStatus());
        assertTrue(result.isTruncated());
        assertEquals(80, result.getCharacterCount());
        assertEquals(80, result.getText().length());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().get(0).contains("safely bounded during extraction"));
    }

    @Test
    @DisplayName("3. Full text is not accumulated before truncation; bounded writer enforces hard limit")
    void testBoundedStringWriterEnforcesHardCeiling() throws IOException {
        SecurePdfExtractor.BoundedStringWriter writer = new SecurePdfExtractor.BoundedStringWriter(25);
        char[] data = "123456789012345678901234567890".toCharArray(); // 30 chars

        boolean thrown = false;
        try {
            writer.write(data, 0, 30);
        } catch (SecurePdfExtractor.CharacterLimitExceededException e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals(25, writer.length());
        assertEquals(25, writer.toString().length());
    }

    @Test
    @DisplayName("4. Sensitive numeric preservation: UPI and 12-16 digit UTR numbers are preserved unmutated")
    void testPreserveUpiAndUtrNumbers() throws IOException {
        String originalNarration = "UPI/308115629124/Groceries Blinkit IMPS/509214002931/Transfer";
        byte[] pdf = createTestPdf(1, originalNarration);

        PdfExtractionResult result = extractor.extract(pdf);

        assertTrue(result.isSuccess());
        // Verify exact preservation of 12-16 digit reference numbers
        assertTrue(result.getText().contains("308115629124"));
        assertTrue(result.getText().contains("509214002931"));
        assertFalse(result.getText().contains("XXXX"));
    }

    @Test
    @DisplayName("5. Numeric preservation: Dates and monetary amounts are never mutated")
    void testPreserveDatesAndMonetaryAmounts() throws IOException {
        String numbers = "DATE 20260301 AMOUNT 150000.50 BALANCE 450000.75 ACC 987654321098";
        byte[] pdf = createTestPdf(1, numbers);

        PdfExtractionResult result = extractor.extract(pdf);

        assertTrue(result.isSuccess());
        assertTrue(result.getText().contains("20260301"));
        assertTrue(result.getText().contains("150000.50"));
        assertTrue(result.getText().contains("450000.75"));
        assertTrue(result.getText().contains("987654321098"));
    }

    @Test
    @DisplayName("6. Streaming InputStream extraction without unbounded byte buffering")
    void testDirectInputStreamStreaming() throws IOException {
        byte[] pdf = createTestPdf(2, "AXIS BANK DIGITAL STATEMENT");
        InputStream stream = new ByteArrayInputStream(pdf);

        PdfExtractionResult result = extractor.extract(stream);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getPageCount());
        assertTrue(result.getText().contains("AXIS BANK"));
    }

    @Test
    @DisplayName("7. Oversized upload stream terminates early and returns OVERSIZED_INPUT")
    void testOversizedUploadStream() {
        SecurePdfExtractor tinyExtractor = new SecurePdfExtractor(500, 10, 1000);
        byte[] largeBytes = new byte[1500];
        largeBytes[0] = '%';
        largeBytes[1] = 'P';
        largeBytes[2] = 'D';
        largeBytes[3] = 'F';
        largeBytes[4] = '-';

        InputStream stream = new ByteArrayInputStream(largeBytes);
        PdfExtractionResult result = tinyExtractor.extract(stream);

        assertFalse(result.isSuccess());
        assertEquals(PdfExtractionResult.Status.OVERSIZED_INPUT, result.getStatus());
        assertTrue(result.getErrorMessage().contains("exceeds the maximum allowed size"));
    }

    @Test
    @DisplayName("8. Password-protected PDF is rejected with PASSWORD_PROTECTED status")
    void testPasswordProtectedPdf() throws IOException {
        byte[] encryptedPdf = createEncryptedPdf("pass123", "Confidential Statement Data");
        PdfExtractionResult result = extractor.extract(encryptedPdf);

        assertFalse(result.isSuccess());
        assertEquals(PdfExtractionResult.Status.PASSWORD_PROTECTED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("password-protected"));
    }

    @Test
    @DisplayName("9. Insufficient extractable text uses neutral wording (INSUFFICIENT_DIGITAL_TEXT)")
    void testInsufficientDigitalTextNeutralWording() throws IOException {
        // Blank page has zero extractable text
        byte[] blankPdf = createTestPdf(1, "");
        PdfExtractionResult result = extractor.extract(blankPdf);

        assertFalse(result.isSuccess());
        assertEquals(PdfExtractionResult.Status.INSUFFICIENT_DIGITAL_TEXT, result.getStatus());
        assertTrue(result.getErrorMessage().contains("No readable digital text layer was detected"));
        assertFalse(result.getErrorMessage().contains("scanned image or photo")); // Neutral status
    }

    @Test
    @DisplayName("10. Multi-page PDF extraction and accurate page counts")
    void testMultiPagePdfAccuracy() throws IOException {
        byte[] pdf = createTestPdf(4, "STATE BANK OF INDIA");
        PdfExtractionResult result = extractor.extract(pdf);

        assertTrue(result.isSuccess());
        assertEquals(4, result.getPageCount());
        assertTrue(result.getText().contains("Page 1"));
        assertTrue(result.getText().contains("Page 4"));
    }

    @Test
    @DisplayName("11. Empty PDF input returns EMPTY_INPUT")
    void testEmptyPdfInput() {
        PdfExtractionResult resNull = extractor.extract((byte[]) null);
        assertEquals(PdfExtractionResult.Status.EMPTY_INPUT, resNull.getStatus());

        PdfExtractionResult resStreamNull = extractor.extract((InputStream) null);
        assertEquals(PdfExtractionResult.Status.EMPTY_INPUT, resStreamNull.getStatus());
    }

    @Test
    @DisplayName("12. Invalid PDF signature returns INVALID_SIGNATURE")
    void testInvalidPdfSignature() {
        byte[] badBytes = "CORRUPTED_NON_PDF_DATA".getBytes();
        PdfExtractionResult res = extractor.extract(badBytes);

        assertFalse(res.isSuccess());
        assertEquals(PdfExtractionResult.Status.INVALID_SIGNATURE, res.getStatus());
    }
}
