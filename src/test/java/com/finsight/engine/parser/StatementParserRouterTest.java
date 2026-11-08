package com.finsight.engine.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.parser.StatementParserRouter.CandidateScore;
import com.finsight.engine.parser.StatementParserRouter.RoutingDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StatementParserRouter & Foundation Contract Tests")
public class StatementParserRouterTest {

    private StatementParserRouter router;

    /**
     * Synthetic test parser fixture simulating bank detection and parsing behavior.
     */
    private static class MockBankParser implements BankStatementParser {
        private final String formatId;
        private final String bankName;
        private final String requiredHeader;
        private final BigDecimal fixedConfidence;

        public MockBankParser(String formatId, String bankName, String requiredHeader, BigDecimal fixedConfidence) {
            this.formatId = formatId;
            this.bankName = bankName;
            this.requiredHeader = requiredHeader;
            this.fixedConfidence = fixedConfidence;
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
            if (statementText != null && statementText.contains(requiredHeader)) {
                return fixedConfidence;
            }
            return BigDecimal.ZERO;
        }

        @Override
        public StatementParseResult parse(String statementText) {
            List<ParsedTransaction> txs = new ArrayList<>();
            List<String> unparsed = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            String[] lines = statementText.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains("TXN:")) {
                    ParsedTransaction tx = new ParsedTransaction(
                            "tx-1", LocalDate.of(2026, 3, 1), "Grocery Purchase",
                            new BigDecimal("1500.00"), null, new BigDecimal("1500.00"),
                            TransactionType.EXPENSE, new BigDecimal("48500.00"),
                            "Store", "Food & Dining", new BigDecimal("0.95"),
                            line, formatId, false, false, false, false
                    );
                    txs.add(tx);
                } else if (!line.trim().isEmpty() && !line.contains(requiredHeader)) {
                    unparsed.add(line);
                }
            }

            if (unparsed.size() > 0) {
                warnings.add("Some lines could not be parsed: " + unparsed.size() + " lines");
            }

            return StatementParseResult.success(formatId, fixedConfidence, txs, unparsed, warnings);
        }
    }

    @BeforeEach
    void setUp() {
        router = StatementParserRouter.createEmpty();
    }

    @Test
    @DisplayName("1. Correct parser selection when a registered parser identifies format with high confidence")
    void testCorrectParserSelection() {
        MockBankParser hdfcParser = new MockBankParser("HDFC_V1", "HDFC Bank", "HDFC_HEADER_TOKEN", new BigDecimal("0.95"));
        MockBankParser sbiParser = new MockBankParser("SBI_V1", "State Bank of India", "SBI_HEADER_TOKEN", new BigDecimal("0.90"));

        router.registerParser(hdfcParser);
        router.registerParser(sbiParser);

        String sampleText = "HDFC_HEADER_TOKEN\nTXN: 2026-03-01 1500.00 GROCERY";
        RoutingDecision decision = router.route(sampleText);

        assertTrue(decision.isRouted());
        assertEquals(hdfcParser, decision.getSelectedParser());
        assertEquals(new BigDecimal("0.95"), decision.getConfidence());
        assertEquals(StatementParseResult.Status.SUCCESS, decision.getStatus());
        assertNull(decision.getFailureReason());

        StatementParseResult result = router.parse(sampleText);
        assertTrue(result.isSuccess());
        assertEquals("HDFC_V1", result.getBankFormat());
        assertEquals(1, result.getParsedRowCount());
    }

    @Test
    @DisplayName("2. Unsupported format handling when no parser matches structural criteria")
    void testUnsupportedFormatHandling() {
        MockBankParser hdfcParser = new MockBankParser("HDFC_V1", "HDFC Bank", "HDFC_HEADER_TOKEN", new BigDecimal("0.95"));
        router.registerParser(hdfcParser);

        String unknownText = "RANDOM INVOICE DOCUMENT WITHOUT BANK HEADERS";
        RoutingDecision decision = router.route(unknownText);

        assertFalse(decision.isRouted());
        assertNull(decision.getSelectedParser());
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, decision.getStatus());
        assertTrue(decision.getFailureReason().contains("not recognized"));

        StatementParseResult result = router.parse(unknownText);
        assertFalse(result.isSuccess());
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, result.getStatus());
    }

    @Test
    @DisplayName("3. Ambiguous format handling when two parsers score within ambiguity threshold")
    void testAmbiguousFormatHandling() {
        // Two parsers scoring 0.90 and 0.92 (diff = 0.02 <= 0.05 AMBIGUITY_DELTA)
        MockBankParser parserA = new MockBankParser("BANK_A", "Bank Alpha", "SHARED_HEADER", new BigDecimal("0.92"));
        MockBankParser parserB = new MockBankParser("BANK_B", "Bank Beta", "SHARED_HEADER", new BigDecimal("0.90"));

        router.registerParser(parserA);
        router.registerParser(parserB);

        String ambiguousText = "SHARED_HEADER\nTXN: Details";
        RoutingDecision decision = router.route(ambiguousText);

        assertFalse(decision.isRouted());
        assertNull(decision.getSelectedParser());
        assertEquals(StatementParseResult.Status.AMBIGUOUS_FORMAT, decision.getStatus());
        assertTrue(decision.getFailureReason().contains("Ambiguous statement format"));
        assertTrue(decision.getFailureReason().contains("Bank Alpha"));
        assertTrue(decision.getFailureReason().contains("Bank Beta"));
    }

    @Test
    @DisplayName("4. Low confidence detection (< 0.70) is rejected as unsupported")
    void testLowConfidenceDetectionRejected() {
        MockBankParser lowConfParser = new MockBankParser("WEAK_V1", "Weak Bank", "WEAK_HEADER", new BigDecimal("0.55"));
        router.registerParser(lowConfParser);

        String sampleText = "WEAK_HEADER\nSome data";
        RoutingDecision decision = router.route(sampleText);

        assertFalse(decision.isRouted());
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, decision.getStatus());
        assertTrue(decision.getFailureReason().contains("below minimum required threshold"));
    }

    @Test
    @DisplayName("5. Empty or whitespace extracted text returns EMPTY_INPUT status")
    void testEmptyExtractedText() {
        MockBankParser parser = new MockBankParser("HDFC_V1", "HDFC Bank", "HDFC", new BigDecimal("0.90"));
        router.registerParser(parser);

        RoutingDecision decisionNull = router.route(null);
        assertEquals(StatementParseResult.Status.EMPTY_INPUT, decisionNull.getStatus());

        RoutingDecision decisionEmpty = router.route("   \n\t  ");
        assertEquals(StatementParseResult.Status.EMPTY_INPUT, decisionEmpty.getStatus());
    }

    @Test
    @DisplayName("6. StatementParseResult immutability prevents external modification")
    void testParseResultImmutability() {
        List<ParsedTransaction> txs = new ArrayList<>();
        List<String> unparsed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        StatementParseResult result = StatementParseResult.success("MOCK_V1", new BigDecimal("0.95"), txs, unparsed, warnings);

        assertThrows(UnsupportedOperationException.class, () -> result.getTransactions().add(new ParsedTransaction()));
        assertThrows(UnsupportedOperationException.class, () -> result.getRawUnparsedLines().add("New Unparsed Line"));
        assertThrows(UnsupportedOperationException.class, () -> result.getWarnings().add("New Warning"));
    }

    @Test
    @DisplayName("7. Raw source line preservation on parsed transactions")
    void testRawLinePreservation() {
        MockBankParser parser = new MockBankParser("HDFC_V1", "HDFC Bank", "HEADER", new BigDecimal("0.90"));
        router.registerParser(parser);

        String sample = "HEADER\nTXN: 2026-03-01 1500.00 GROCERY";
        StatementParseResult result = router.parse(sample);

        assertEquals(1, result.getTransactions().size());
        ParsedTransaction tx = result.getTransactions().get(0);
        assertEquals("TXN: 2026-03-01 1500.00 GROCERY", tx.getRawLine());
        assertEquals("HDFC_V1", tx.getBankFormat());
    }

    @Test
    @DisplayName("8. Rejected-row and warning accounting accurately reflected in result")
    void testRejectedRowAndWarningAccounting() {
        MockBankParser parser = new MockBankParser("HDFC_V1", "HDFC Bank", "HEADER", new BigDecimal("0.90"));
        router.registerParser(parser);

        String sampleWithUnparsed = "HEADER\nTXN: 2026-03-01 1500.00 GROCERY\nFOOTER: Terms & Conditions Page 1";
        StatementParseResult result = router.parse(sampleWithUnparsed);

        assertEquals(StatementParseResult.Status.PARTIALLY_PARSED, result.getStatus());
        assertEquals(1, result.getParsedRowCount());
        assertEquals(1, result.getRejectedRowCount());
        assertEquals(1, result.getRawUnparsedLines().size());
        assertTrue(result.getRawUnparsedLines().get(0).contains("FOOTER: Terms"));
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("9. No parser registered produces clean UNSUPPORTED_FORMAT without false success")
    void testNoParsersRegisteredProducesUnsupported() {
        // Router with 0 registered parsers
        StatementParserRouter emptyRouter = StatementParserRouter.createEmpty();
        RoutingDecision decision = emptyRouter.route("SOME VALID STATEMENT CONTENT");

        assertFalse(decision.isRouted());
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, decision.getStatus());
        assertTrue(decision.getFailureReason().contains("No statement parsers are registered"));

        StatementParseResult result = emptyRouter.parse("SOME VALID STATEMENT CONTENT");
        assertFalse(result.isSuccess());
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, result.getStatus());
    }
}

