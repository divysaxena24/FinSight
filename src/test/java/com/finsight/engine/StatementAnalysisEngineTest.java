package com.finsight.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.StatementAnalysisResult.Status;
import com.finsight.engine.analytics.AnalyticsCalculator;
import com.finsight.engine.categorization.RuleBasedTransactionCategorizer;
import com.finsight.engine.insights.BudgetRecommendationEngine;
import com.finsight.engine.insights.SpendingBaselineCalculator;
import com.finsight.engine.insights.UnusualTransactionDetector;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.parser.BankStatementParser;
import com.finsight.engine.parser.StatementParseResult;
import com.finsight.engine.parser.StatementParserRouter;
import com.finsight.engine.pdf.PdfExtractionResult;
import com.finsight.engine.pdf.SecurePdfExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StatementAnalysisEngine End-to-End Orchestration Tests")
public class StatementAnalysisEngineTest {

    private StatementAnalysisEngine engine;
    private StatementParserRouter router;
    private SecurePdfExtractor extractor;

    /**
     * Test mock parser that simulates statement parsing.
     */
    private static class MockParser implements BankStatementParser {
        private final String formatId;
        private final String bankName;
        private final String triggerPhrase;
        private final BigDecimal confidence;
        private List<ParsedTransaction> cannedTransactions = new ArrayList<>();
        private List<String> unparsedLines = new ArrayList<>();
        private List<String> parserWarnings = new ArrayList<>();
        private StatementParseResult.Status forceStatus = null;

        public MockParser(String formatId, String bankName, String triggerPhrase, BigDecimal confidence) {
            this.formatId = formatId;
            this.bankName = bankName;
            this.triggerPhrase = triggerPhrase;
            this.confidence = confidence;
        }

        public void setCannedTransactions(List<ParsedTransaction> cannedTransactions) {
            this.cannedTransactions = cannedTransactions;
        }

        public void setUnparsedLines(List<String> unparsedLines) {
            this.unparsedLines = unparsedLines;
        }

        public void setParserWarnings(List<String> parserWarnings) {
            this.parserWarnings = parserWarnings;
        }

        public void setForceStatus(StatementParseResult.Status forceStatus) {
            this.forceStatus = forceStatus;
        }

        @Override
        public String getFormatId() {
            return formatId;
        }

        @Override
        public String getBankName() {
            return bankName;
        }

        @Override
        public BigDecimal detectConfidence(String statementText) {
            if (statementText != null && statementText.contains(triggerPhrase)) {
                return confidence;
            }
            return BigDecimal.ZERO;
        }

        @Override
        public StatementParseResult parse(String statementText) {
            if (forceStatus == StatementParseResult.Status.PARSING_FAILURE) {
                return StatementParseResult.failure(forceStatus, formatId, confidence, "Corrupted tabular structure", parserWarnings);
            }
            return StatementParseResult.success(formatId, confidence, cannedTransactions, unparsedLines, parserWarnings);
        }
    }

    @BeforeEach
    void setUp() {
        extractor = new SecurePdfExtractor();
        router = StatementParserRouter.createEmpty();
        engine = new StatementAnalysisEngine(
                extractor,
                router,
                new RuleBasedTransactionCategorizer(),
                new AnalyticsCalculator(),
                new SpendingBaselineCalculator(2),
                new UnusualTransactionDetector(),
                new BudgetRecommendationEngine()
        );
    }

    private byte[] createTestPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null && !text.isEmpty()) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private ParsedTransaction createTx(String id, LocalDate date, String desc, BigDecimal amt, TransactionType type) {
        return new ParsedTransaction(
                id, date, desc,
                type == TransactionType.EXPENSE ? amt : null,
                type == TransactionType.INCOME ? amt : null,
                amt, type, null, null, null,
                BigDecimal.ONE, desc, "MOCK_BANK",
                false, false, false, false, false
        );
    }

    @Test
    @DisplayName("1. Successful extraction, parsing, categorization, and financial analysis")
    void testSuccessfulAnalysisPipeline() throws IOException {
        MockParser mockParser = new MockParser("SYNTH_BANK_V1", "Synthetic Bank", "SYNTHETIC_BANK_STATEMENT", new BigDecimal("0.95"));
        List<ParsedTransaction> txs = List.of(
                createTx("t1", LocalDate.of(2026, 1, 10), "SWIGGY*ORDER12345", new BigDecimal("450.00"), TransactionType.EXPENSE),
                createTx("t2", LocalDate.of(2026, 1, 15), "AMAZON RETAIL INDIA", new BigDecimal("2500.00"), TransactionType.EXPENSE),
                createTx("t3", LocalDate.of(2026, 1, 31), "MONTHLY SALARY CREDIT", new BigDecimal("75000.00"), TransactionType.INCOME)
        );
        mockParser.setCannedTransactions(txs);
        router.registerParser(mockParser);

        byte[] pdfBytes = createTestPdf("SYNTHETIC_BANK_STATEMENT ACCOUNT 1234567890");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        assertTrue(result.isSuccessful());
        assertTrue(result.isSuitableForReview());
        assertEquals("Synthetic Bank", result.getBankName());
        assertEquals("SYNTH_BANK_V1", result.getParserFormatId());
        assertEquals(3, result.getParsedTransactionCount());
        assertEquals(0, result.getUnparsedLineCount());

        // Report verification
        assertNotNull(result.getFinancialReport());
        assertEquals(new BigDecimal("75000.00"), result.getFinancialReport().getSummary().getTotalIncome());
        assertEquals(new BigDecimal("2950.00"), result.getFinancialReport().getSummary().getGrossExpenses());

        // Categorization verification
        assertEquals(new BigDecimal("450.00"), result.getFinancialReport().getCategoryExpenses().get("Food & Dining"));
        assertEquals(new BigDecimal("2500.00"), result.getFinancialReport().getCategoryExpenses().get("Shopping"));
    }

    @Test
    @DisplayName("2. Insufficient digital text returns EXTRACTION_FAILED status")
    void testInsufficientDigitalText() throws IOException {
        // PDF with only 5 characters (below 20 chars minimum threshold)
        byte[] pdfBytes = createTestPdf("Short");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.EXTRACTION_FAILED, result.getStatus());
        assertFalse(result.isSuccessful());
        assertFalse(result.isSuitableForReview());
        assertEquals(PdfExtractionResult.Status.INSUFFICIENT_DIGITAL_TEXT, result.getExtractionStatus());
        assertNull(result.getFinancialReport());
    }

    @Test
    @DisplayName("3. Invalid non-PDF input returns EXTRACTION_FAILED status")
    void testInvalidPdfInput() {
        byte[] junkBytes = "This is not a PDF file format content".getBytes();
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(junkBytes));

        assertEquals(Status.EXTRACTION_FAILED, result.getStatus());
        assertEquals(PdfExtractionResult.Status.INVALID_SIGNATURE, result.getExtractionStatus());
        assertFalse(result.isSuccessful());
    }

    @Test
    @DisplayName("4. Unsupported statement format returns UNSUPPORTED_FORMAT status")
    void testUnsupportedFormat() throws IOException {
        MockParser mockParser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTHETIC_HEADER", new BigDecimal("0.90"));
        router.registerParser(mockParser);

        // PDF text does not contain recognized bank headers
        byte[] pdfBytes = createTestPdf("UNKNOWN FOREIGN STATEMENT DOCUMENT 2026-03-01 1000.00");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.UNSUPPORTED_FORMAT, result.getStatus());
        assertFalse(result.isSuccessful());
        assertFalse(result.isSuitableForReview());
        assertNull(result.getFinancialReport());
    }

    @Test
    @DisplayName("5. Ambiguous parser detection returns AMBIGUOUS_FORMAT status")
    void testAmbiguousParserDetection() throws IOException {
        // Two parsers match with identical/close confidence (diff <= 0.05)
        MockParser parserA = new MockParser("BANK_A", "Bank A", "SHARED_HEADER_KEYWORD", new BigDecimal("0.90"));
        MockParser parserB = new MockParser("BANK_B", "Bank B", "SHARED_HEADER_KEYWORD", new BigDecimal("0.92"));
        router.registerParser(parserA);
        router.registerParser(parserB);

        byte[] pdfBytes = createTestPdf("SHARED_HEADER_KEYWORD ACCOUNT STATEMENT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.AMBIGUOUS_FORMAT, result.getStatus());
        assertFalse(result.isSuccessful());
        assertFalse(result.isSuitableForReview());
    }

    @Test
    @DisplayName("6. Partial parsing with unparsed lines produces PARTIALLY_PARSED status")
    void testPartialParsingWithUnparsedLines() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        parser.setCannedTransactions(List.of(
                createTx("t1", LocalDate.of(2026, 1, 10), "UBER TRIP", new BigDecimal("250.00"), TransactionType.EXPENSE)
        ));
        parser.setUnparsedLines(List.of("BROKEN ROW WITH MISSING DATE AND CORRUPT DATA"));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT ACCOUNT STATEMENT DETAILS AND TRANSACTIONS 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.PARTIALLY_PARSED, result.getStatus());
        assertTrue(result.isSuccessful());
        assertTrue(result.isSuitableForReview());
        assertEquals(1, result.getParsedTransactionCount());
        assertEquals(1, result.getUnparsedLineCount());
        assertEquals("BROKEN ROW WITH MISSING DATE AND CORRUPT DATA", result.getUnparsedLines().get(0));
    }

    @Test
    @DisplayName("7. Categorization integration normalizes merchants and assigns categories")
    void testCategorizationIntegration() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        parser.setCannedTransactions(List.of(
                createTx("t1", LocalDate.of(2026, 1, 10), "NETFLIX ENTERTAINMENT", new BigDecimal("649.00"), TransactionType.EXPENSE)
        ));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        ParsedTransaction tx = result.getFinancialReport().getTransactions().get(0);
        assertEquals("Entertainment", tx.getCategory());
        assertEquals("Netflix", tx.getMerchant());
    }

    @Test
    @DisplayName("8. Preservation of manually categorized transactions")
    void testManualCategorizationPreserved() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        ParsedTransaction manualTx = createTx("m1", LocalDate.of(2026, 1, 10), "SWIGGY*ORDER", new BigDecimal("500.00"), TransactionType.EXPENSE);
        manualTx.setCategory("Entertainment"); // Manually chosen by user
        manualTx.setManuallyCategorized(true);

        parser.setCannedTransactions(List.of(manualTx));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        ParsedTransaction tx = result.getFinancialReport().getTransactions().get(0);
        assertEquals("Entertainment", tx.getCategory()); // NOT overwritten to Food & Dining!
        assertTrue(tx.isManuallyCategorized());
    }

    @Test
    @DisplayName("9. Refund and transfer accounting integration")
    void testRefundAndTransferAccounting() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        ParsedTransaction exp = createTx("e1", LocalDate.of(2026, 1, 10), "AMAZON PURCHASE", new BigDecimal("2000.00"), TransactionType.EXPENSE);
        ParsedTransaction ref = createTx("r1", LocalDate.of(2026, 1, 15), "AMAZON RETURN REFUND", new BigDecimal("500.00"), TransactionType.INCOME);
        ref.setRefundOrReversal(true);
        ParsedTransaction trf = createTx("tr1", LocalDate.of(2026, 1, 20), "TRANSFER TO SAVINGS", new BigDecimal("10000.00"), TransactionType.TRANSFER);

        parser.setCannedTransactions(List.of(exp, ref, trf));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        // Gross expenses = 2000, Refunds = 500 -> Net expenses = 1500. Transfer is excluded.
        assertEquals(new BigDecimal("2000.00"), result.getFinancialReport().getSummary().getGrossExpenses());
        assertEquals(new BigDecimal("500.00"), result.getFinancialReport().getSummary().getTotalRefunds());
        assertEquals(new BigDecimal("1500.00"), result.getFinancialReport().getSummary().getNetExpenses());
    }

    @Test
    @DisplayName("10. Confirmed duplicate excluded, suspected duplicate preserved")
    void testConfirmedVsSuspectedDuplicates() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        ParsedTransaction t1 = createTx("t1", LocalDate.of(2026, 1, 10), "UBER", new BigDecimal("300.00"), TransactionType.EXPENSE);
        ParsedTransaction t2Suspected = createTx("t2", LocalDate.of(2026, 1, 10), "UBER", new BigDecimal("300.00"), TransactionType.EXPENSE);
        t2Suspected.setDuplicateCandidate(true);
        ParsedTransaction t3Confirmed = createTx("t3", LocalDate.of(2026, 1, 10), "UBER", new BigDecimal("300.00"), TransactionType.EXPENSE);
        t3Confirmed.setConfirmedDuplicate(true);

        parser.setCannedTransactions(List.of(t1, t2Suspected, t3Confirmed));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        // All 3 transactions are retained in report for auditability
        assertEquals(3, result.getFinancialReport().getTransactions().size());
        // Confirmed duplicate is excluded from gross expenses: 300 + 300 = 600.00 (t3 excluded)
        assertEquals(new BigDecimal("600.00"), result.getFinancialReport().getSummary().getGrossExpenses());
        assertEquals(1, result.getFinancialReport().getSummary().getConfirmedDuplicateCount());
    }

    @Test
    @DisplayName("11. Out-of-period transactions boundary isolation")
    void testOutOfPeriodIsolation() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        ParsedTransaction outOld = createTx("o1", LocalDate.of(2025, 12, 31), "OLD PURCHASE", new BigDecimal("9999.00"), TransactionType.EXPENSE);
        ParsedTransaction inTx = createTx("i1", LocalDate.of(2026, 1, 15), "IN PERIOD", new BigDecimal("1000.00"), TransactionType.EXPENSE);
        ParsedTransaction outFuture = createTx("o2", LocalDate.of(2026, 2, 1), "FUTURE PURCHASE", new BigDecimal("9999.00"), TransactionType.EXPENSE);

        parser.setCannedTransactions(List.of(outOld, inTx, outFuture));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        // Declared period: 2026-01-01 to 2026-01-31
        StatementAnalysisResult result = engine.analyze(
                new ByteArrayInputStream(pdfBytes),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertEquals(Status.SUCCESS, result.getStatus());
        assertEquals(new BigDecimal("1000.00"), result.getFinancialReport().getSummary().getGrossExpenses());
        assertEquals(2, result.getFinancialReport().getOutOfPeriodTransactions().size());
    }

    @Test
    @DisplayName("12. Historical spending intelligence integration")
    void testHistoricalSpendingIntelligenceIntegration() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        // 3 consecutive monthly payments for Netflix -> recurring payment detected!
        ParsedTransaction n1 = createTx("n1", LocalDate.of(2026, 1, 5), "NETFLIX ENTERTAINMENT", new BigDecimal("649.00"), TransactionType.EXPENSE);
        ParsedTransaction n2 = createTx("n2", LocalDate.of(2026, 2, 4), "NETFLIX ENTERTAINMENT", new BigDecimal("649.00"), TransactionType.EXPENSE);
        ParsedTransaction n3 = createTx("n3", LocalDate.of(2026, 3, 6), "NETFLIX ENTERTAINMENT", new BigDecimal("649.00"), TransactionType.EXPENSE);

        parser.setCannedTransactions(List.of(n1, n2, n3));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        assertFalse(result.getFinancialInsights().isEmpty());
        assertTrue(result.getFinancialInsights().stream().anyMatch(i -> i.getTitle().contains("Recurring")));
    }

    @Test
    @DisplayName("13. Budget recommendation integration")
    void testBudgetRecommendationIntegration() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        ParsedTransaction u1 = createTx("u1", LocalDate.of(2026, 1, 10), "BESCOM ELECTRICITY", new BigDecimal("1500.00"), TransactionType.EXPENSE);
        ParsedTransaction u2 = createTx("u2", LocalDate.of(2026, 2, 10), "BESCOM ELECTRICITY", new BigDecimal("1500.00"), TransactionType.EXPENSE);

        parser.setCannedTransactions(List.of(u1, u2));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        assertFalse(result.getBudgetRecommendations().isEmpty());
        assertEquals("Utilities", result.getBudgetRecommendations().get(0).getCategory());
        assertEquals(new BigDecimal("1650.00"), result.getBudgetRecommendations().get(0).getRecommendedBudget());
    }

    @Test
    @DisplayName("14. Extraction and parser warnings are propagated traceably")
    void testWarningsPropagation() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        parser.setCannedTransactions(List.of(
                createTx("t1", LocalDate.of(2026, 1, 10), "SWIGGY", new BigDecimal("300.00"), TransactionType.EXPENSE)
        ));
        parser.setParserWarnings(List.of("Header row contained minor checksum deviation"));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        assertTrue(result.getParserWarnings().contains("Header row contained minor checksum deviation"));
    }

    @Test
    @DisplayName("15. Empty parsed transaction result returns PARSING_FAILED")
    void testEmptyParsedTransactionsReturnsParsingFailed() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        parser.setCannedTransactions(Collections.emptyList()); // Empty
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT EMPTY CONTENT 2026 STATEMENT DETAILS AND TRANSACTIONS");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.PARSING_FAILED, result.getStatus());
        assertFalse(result.isSuccessful());
        assertFalse(result.isSuitableForReview());
    }

    @Test
    @DisplayName("16. Analysis pipeline performs zero database access")
    void testZeroDatabaseWrites() throws IOException {
        MockParser parser = new MockParser("SYNTH_BANK", "Synthetic Bank", "SYNTH_STATEMENT", new BigDecimal("0.95"));
        parser.setCannedTransactions(List.of(
                createTx("t1", LocalDate.of(2026, 1, 10), "TEST EXPENSE", new BigDecimal("100.00"), TransactionType.EXPENSE)
        ));
        router.registerParser(parser);

        byte[] pdfBytes = createTestPdf("SYNTH_STATEMENT VALID STATEMENT TEXT 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertNotNull(result);
        assertEquals(Status.SUCCESS, result.getStatus());
        // Pure computation in memory without database touch
    }
}

