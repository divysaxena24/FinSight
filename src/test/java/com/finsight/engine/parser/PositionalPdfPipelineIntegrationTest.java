package com.finsight.engine.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.StatementAnalysisEngine;
import com.finsight.engine.StatementAnalysisResult;
import com.finsight.engine.StatementAnalysisResult.Status;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.staging.StagingSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Step 16.2: End-to-End Positional Statement PDF Integration Tests")
public class PositionalPdfPipelineIntegrationTest {

    private StatementAnalysisEngine engine;

    @BeforeEach
    void setUp() {
        engine = new StatementAnalysisEngine();
    }

    private byte[] createMultiLinePdf(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(40, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("End-to-End PDF pipeline extracts and stages the observed positional statement format")
    void testEndToEndPositionalPdf() throws IOException {
        List<String> lines = List.of(
                "SAVINGS ACCOUNT STATEMENT",
                "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Weekly household groceries 22,654.50",
                "2026-10-10 CARD Fuel Pump Station Transport Debit 2,500.00 Petrol fill 20,154.50",
                "2026-10-11 BBPS Electricity Board Utilities Debit 1,200.00 Power bill 18,954.50",
                "2026-10-12 IMPS Client Retainer Salary Credit 45,000.00 Professional services 63,954.50"
        );

        byte[] pdfBytes = createMultiLinePdf(lines);
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertTrue(result.isSuccessful(), "Analysis should succeed: " + result.getFailureMessage());
        assertEquals(Status.SUCCESS, result.getStatus());
        assertEquals(GenericBankStatementParser.FORMAT_ID, result.getParserFormatId());
        assertEquals(GenericBankStatementParser.BANK_NAME, result.getBankName());
        assertEquals(4, result.getParsedTransactionCount());
        assertEquals(0, result.getUnparsedLineCount());
        assertTrue(result.isSuitableForReview());

        // Validate Report
        FinancialStatementReport report = result.getFinancialReport();
        assertNotNull(report);
        assertEquals(new BigDecimal("45000.00"), report.getSummary().getTotalIncome());
        assertEquals(new BigDecimal("5545.50"), report.getSummary().getGrossExpenses());
        assertEquals(new BigDecimal("39454.50"), report.getSummary().getNetCashFlow());

        // Verify Staging Integration
        StagingSession staging = new StagingSession(1, result, 3600);
        assertEquals(4, staging.getTotalCount());
        assertEquals(4, staging.getSelectedCount());

        List<StagingSession.StagedTransactionItem> items = staging.getItemsList();
        ParsedTransaction t1 = items.get(0).getTransaction();
        assertEquals(LocalDate.of(2026, 10, 9), t1.getTransactionDate());
        assertEquals("FreshMart Supermarket", t1.getMerchant());
        assertEquals("Groceries", t1.getCategory());
        assertEquals(TransactionType.EXPENSE, t1.getTransactionType());
        assertEquals(new BigDecimal("1845.50"), t1.getAmount());
        assertEquals(new BigDecimal("22654.50"), t1.getBalance());
        assertEquals("FreshMart Supermarket - Weekly household groceries", t1.getDescription());
    }
}

