package com.finsight.engine.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.finsight.engine.StatementAnalysisEngine;
import com.finsight.engine.StatementAnalysisResult;
import com.finsight.engine.StatementAnalysisResult.Status;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.pdf.SecurePdfExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Generic Parser End-to-End Pipeline Integration Tests")
public class GenericParserIntegrationTest {

    private StatementAnalysisEngine engine;

    @BeforeEach
    void setUp() {
        // Engine defaults to StatementParserRouter() which automatically registers GenericBankStatementParser
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
    @DisplayName("1. Complete PDF pipeline: Extract -> Route -> Generic Parse -> Categorize -> Analytics -> Report")
    void testEndToEndPipelineWithGenericParser() throws IOException {
        List<String> pdfLines = List.of(
                "SAVINGS ACCOUNT STATEMENT",
                "Date | Narration | Debit | Credit | Balance",
                "01/10/2026 | Infosys Monthly Salary | | 95,000.00 | 1,45,000.00",
                "03/10/2026 | Swiggy Food Delivery | 750.00 | | 1,44,250.00",
                "05/10/2026 | Amazon India Retail | 2,499.00 | | 1,41,751.00",
                "10/10/2026 | Uber City Taxi Ride | 450.00 | | 1,41,301.00"
        );

        byte[] pdfBytes = createMultiLinePdf(pdfLines);
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertTrue(result.isSuccessful(), "Analysis should succeed: " + result.getFailureMessage());
        assertEquals(Status.SUCCESS, result.getStatus());
        assertEquals(GenericBankStatementParser.FORMAT_ID, result.getParserFormatId());
        assertEquals(GenericBankStatementParser.BANK_NAME, result.getBankName());
        assertEquals(4, result.getParsedTransactionCount());
        assertTrue(result.isSuitableForReview());

        // Validate Financial Report
        FinancialStatementReport report = result.getFinancialReport();
        assertNotNull(report);
        assertEquals(new BigDecimal("95000.00"), report.getSummary().getTotalIncome());
        assertEquals(new BigDecimal("3699.00"), report.getSummary().getGrossExpenses());
        assertEquals(new BigDecimal("91301.00"), report.getSummary().getNetCashFlow());
        assertEquals(4, report.getSummary().getTotalTransactionCount());

        // Validate Categorization
        assertEquals(new BigDecimal("750.00"), report.getCategoryExpenses().get("Food & Dining"));
        assertEquals(new BigDecimal("2499.00"), report.getCategoryExpenses().get("Shopping"));
        assertEquals(new BigDecimal("450.00"), report.getCategoryExpenses().get("Transport"));
    }

    @Test
    @DisplayName("2. Unrecognized document layout safely rejected as UNSUPPORTED_FORMAT without false match")
    void testUnrecognizedLayoutRejected() throws IOException {
        List<String> pdfLines = List.of(
                "PURCHASE ORDER # 59402",
                "Vendor: Acme Supply Co.",
                "Deliver To: Warehouse 4",
                "Terms: Net 30"
        );

        byte[] pdfBytes = createMultiLinePdf(pdfLines);
        StatementAnalysisResult result = engine.analyze(new ByteArrayInputStream(pdfBytes));

        assertEquals(Status.UNSUPPORTED_FORMAT, result.getStatus());
        assertFalse(result.isSuccessful());
        assertFalse(result.isSuitableForReview());
    }
}
