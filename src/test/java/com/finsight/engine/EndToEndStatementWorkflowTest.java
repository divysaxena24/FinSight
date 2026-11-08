package com.finsight.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import com.expense.model.Transaction;
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
import com.finsight.engine.pdf.SecurePdfExtractor;
import com.finsight.engine.service.HistoricalTransactionAdapter;
import com.finsight.engine.service.StatementImportService;
import com.finsight.engine.service.StatementImportService.ImportResult;
import com.finsight.engine.staging.StagingManager;
import com.finsight.engine.staging.StagingSession;
import com.finsight.engine.staging.StagingSession.ImportStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FinSight End-to-End Workflow & Integration Tests")
public class EndToEndStatementWorkflowTest {

    private StatementAnalysisEngine engine;
    private StatementParserRouter router;
    private SecurePdfExtractor extractor;
    private StagingManager stagingManager;

    private static class SyntheticBankParser implements BankStatementParser {
        private final String formatId = "SYNTH_BANK_V1";
        private final String bankName = "Synthetic Bank";
        private final BigDecimal confidence = new BigDecimal("0.95");
        private List<ParsedTransaction> cannedTransactions = new ArrayList<>();

        public void setCannedTransactions(List<ParsedTransaction> cannedTransactions) {
            this.cannedTransactions = cannedTransactions;
        }

        @Override
        public String getFormatId() { return formatId; }

        @Override
        public String getBankName() { return bankName; }

        @Override
        public BigDecimal detectConfidence(String statementText) {
            if (statementText != null && statementText.contains("SYNTHETIC_BANK_STATEMENT")) {
                return confidence;
            }
            return BigDecimal.ZERO;
        }

        @Override
        public StatementParseResult parse(String statementText) {
            return StatementParseResult.success(formatId, confidence, cannedTransactions, Collections.emptyList(), Collections.emptyList());
        }
    }

    @BeforeEach
    void setUp() {
        extractor = new SecurePdfExtractor();
        router = new StatementParserRouter();
        engine = new StatementAnalysisEngine(
                extractor,
                router,
                new RuleBasedTransactionCategorizer(),
                new AnalyticsCalculator(),
                new SpendingBaselineCalculator(2),
                new UnusualTransactionDetector(),
                new BudgetRecommendationEngine()
        );
        stagingManager = StagingManager.getInstance();
        stagingManager.clearAll();
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
                BigDecimal.ONE, desc, "SYNTH_BANK",
                false, false, false, false, false
        );
    }

    @Test
    @DisplayName("Complete E2E: PDF -> Analysis -> Staging -> Edit -> Selection -> Import Validation")
    void testCompleteEndToEndPipeline() throws IOException {
        SyntheticBankParser parser = new SyntheticBankParser();
        List<ParsedTransaction> txs = List.of(
                createTx("tx1", LocalDate.of(2026, 2, 10), "SWIGGY*ORDER", new BigDecimal("450.00"), TransactionType.EXPENSE),
                createTx("tx2", LocalDate.of(2026, 2, 15), "AMAZON PURCHASE", new BigDecimal("1200.00"), TransactionType.EXPENSE),
                createTx("tx3", LocalDate.of(2026, 2, 28), "SALARY CREDIT", new BigDecimal("65000.00"), TransactionType.INCOME)
        );
        parser.setCannedTransactions(txs);
        router.registerParser(parser);

        // 1. PDF Upload & Analysis
        byte[] pdfBytes = createTestPdf("SYNTHETIC_BANK_STATEMENT ACCOUNT 9876543210 DETAILS AND TRANSACTIONS 2026");
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getParsedTransactionCount());

        // 2. Server-Side Staging Creation
        int testUserId = 42;
        StagingSession session = stagingManager.createSession(testUserId, result);
        assertNotNull(session);
        assertEquals(testUserId, session.getUserId());
        assertEquals(3, session.getTotalCount());
        assertEquals(3, session.getSelectedCount());
        assertEquals(ImportStatus.PENDING_REVIEW, session.getImportStatus());

        // 3. User Reviews and Edits Transaction Date and Category
        String firstStgTxId = session.getItemsList().get(0).getStagingTxId();
        boolean updated = session.updateTransaction(
                firstStgTxId,
                LocalDate.of(2026, 2, 11),
                "Swiggy Business Dinner",
                "Swiggy",
                new BigDecimal("500.00"),
                "Food & Dining",
                true
        );
        assertTrue(updated);
        assertEquals(new BigDecimal("500.00"), session.getItems().get(firstStgTxId).getTransaction().getAmount());
        assertTrue(session.getItems().get(firstStgTxId).getTransaction().isManuallyCategorized());

        // 4. User Deselects Second Transaction
        String secondStgTxId = session.getItemsList().get(1).getStagingTxId();
        session.updateTransaction(secondStgTxId, null, null, null, null, null, false);
        assertEquals(2, session.getSelectedCount());

        // 5. User Ownership Protection: Other User Cannot Access Session
        int unauthorizedUserId = 99;
        assertNull(stagingManager.getSession(session.getStagingId(), unauthorizedUserId));

        // 6. Mock DAO verifying batch import receives exact selected transactions
        List<Transaction> importedRecords = new ArrayList<>();
        com.expense.dao.TransactionDAO mockDao = new com.expense.dao.TransactionDAO() {
            @Override
            public List<Transaction> getTransactionsByUser(int uId) {
                return Collections.emptyList();
            }

            @Override
            public boolean batchImportTransactions(int uId, List<Transaction> transactions) {
                importedRecords.addAll(transactions);
                return true;
            }
        };

        StatementImportService importService = new StatementImportService(mockDao);
        ImportResult importResult = importService.importStagedTransactions(session, testUserId);

        assertTrue(importResult.isSuccess());
        assertEquals(2, importResult.getImportedCount());
        assertEquals(1, importResult.getSkippedCount()); // Deselected item skipped
        assertEquals(ImportStatus.IMPORTED, session.getImportStatus());

        // Verify imported records match edited values
        assertEquals(2, importedRecords.size());
        assertEquals(500.00, importedRecords.get(0).getAmount(), 0.001);
        assertEquals("Food & Dining", importedRecords.get(0).getCategory());
        assertEquals("Income", importedRecords.get(1).getType());

        // 7. Idempotency: Second Import Attempt is Rejected
        ImportResult duplicateAttempt = importService.importStagedTransactions(session, testUserId);
        assertFalse(duplicateAttempt.isSuccess());
        assertTrue(duplicateAttempt.getMessage().contains("already been imported"));

        // 8. Report Verification: Ensure all KPI getters and collections function cleanly
        com.finsight.engine.model.FinancialStatementReport report = result.getFinancialReport();
        assertNotNull(report);
        assertEquals(new BigDecimal("65000.00"), report.getSummary().getTotalIncome());
        assertEquals(new BigDecimal("1650.00"), report.getSummary().getGrossExpenses());
        assertEquals(new BigDecimal("1650.00"), report.getSummary().getNetExpenses());
        assertEquals(new BigDecimal("63350.00"), report.getSummary().getNetCashFlow());
        assertEquals(new BigDecimal("63350.00"), report.getSummary().getNetSavings());
        assertEquals(3, report.getSummary().getTotalTransactionCount());
        assertNotNull(report.getTransactions());
        assertEquals(3, report.getTransactions().size());
    }
}

