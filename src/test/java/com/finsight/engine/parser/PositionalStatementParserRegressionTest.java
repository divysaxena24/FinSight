package com.finsight.engine.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Step 16.2: Positional Parsing & Failure Investigation Regression Tests")
public class PositionalStatementParserRegressionTest {

    private GenericBankStatementParser parser;

    @BeforeEach
    void setUp() {
        parser = new GenericBankStatementParser();
    }

    @Test
    @DisplayName("1. Single valid debit row parses cleanly")
    void testSingleValidDebitRow() {
        String line = "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Weekly household groceries 22,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2026, 10, 9), tx.getTransactionDate());
        assertEquals("FreshMart Supermarket", tx.getMerchant());
        assertEquals("Groceries", tx.getCategory());
        assertEquals(TransactionType.EXPENSE, tx.getTransactionType());
        assertEquals(new BigDecimal("1845.50"), tx.getDebit());
        assertNull(tx.getCredit());
        assertEquals(new BigDecimal("1845.50"), tx.getAmount());
        assertEquals(new BigDecimal("22654.50"), tx.getBalance());
        assertEquals("FreshMart Supermarket - Weekly household groceries", tx.getDescription());
        assertEquals(line, tx.getRawLine());
    }

    @Test
    @DisplayName("2. Single valid credit row parses cleanly")
    void testSingleValidCreditRow() {
        String line = "2026-10-01 IMPS ACME Corp Payroll Salary Credit 85,000.00 Monthly salary deposit 1,07,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2026, 10, 1), tx.getTransactionDate());
        assertEquals("ACME Corp Payroll", tx.getMerchant());
        assertEquals("Salary", tx.getCategory());
        assertEquals(TransactionType.INCOME, tx.getTransactionType());
        assertEquals(new BigDecimal("85000.00"), tx.getCredit());
        assertNull(tx.getDebit());
        assertEquals(new BigDecimal("85000.00"), tx.getAmount());
        assertEquals(new BigDecimal("107654.50"), tx.getBalance());
        assertEquals("ACME Corp Payroll - Monthly salary deposit", tx.getDescription());
    }

    @Test
    @DisplayName("3. Multiple valid positional rows parsed by statement parser")
    void testMultipleValidPositionalRows() {
        String statementText =
                "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Weekly household groceries 22,654.50\n" +
                "2026-10-10 CARD Fuel Pump Station Transport Debit 2,500.00 Petrol fill 20,154.50\n" +
                "2026-10-11 BBPS Electricity Board Utilities Debit 1,200.00 Power bill 18,954.50\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getParsedRowCount());
        assertEquals(0, result.getRejectedRowCount());

        ParsedTransaction tx1 = result.getTransactions().get(0);
        assertEquals("FreshMart Supermarket", tx1.getMerchant());
        assertEquals("Groceries", tx1.getCategory());

        ParsedTransaction tx2 = result.getTransactions().get(1);
        assertEquals("Fuel Pump Station", tx2.getMerchant());
        assertEquals("Transport", tx2.getCategory());

        ParsedTransaction tx3 = result.getTransactions().get(2);
        assertEquals("Electricity Board", tx3.getMerchant());
        assertEquals("Utilities", tx3.getCategory());
    }

    @Test
    @DisplayName("4. Multi-word merchant and multi-word narration")
    void testMultiWordMerchantAndNarration() {
        String line = "2026-10-12 CARD The Great Indian Bookstore Shopping Debit 950.00 Two technical programming books 18,004.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals("The Great Indian Bookstore", tx.getMerchant());
        assertEquals("Shopping", tx.getCategory());
        assertEquals("The Great Indian Bookstore - Two technical programming books", tx.getDescription());
        assertEquals(new BigDecimal("950.00"), tx.getAmount());
    }

    @Test
    @DisplayName("5. Indian comma-grouped amounts (e.g. 1,50,000.00)")
    void testIndianCommaGroupedAmounts() {
        String line = "2026-10-15 IMPS Flat Owner Rent Housing Debit 1,50,000.00 Apartment rent for October 5,20,000.00";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(new BigDecimal("150000.00"), tx.getAmount());
        assertEquals(new BigDecimal("520000.00"), tx.getBalance());
    }

    @Test
    @DisplayName("6. Western comma-grouped amounts (e.g. 150,000.00)")
    void testWesternCommaGroupedAmounts() {
        String line = "2026-10-15 IMPS Flat Owner Rent Housing Debit 150,000.00 Apartment rent for October 520,000.00";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(new BigDecimal("150000.00"), tx.getAmount());
        assertEquals(new BigDecimal("520000.00"), tx.getBalance());
    }

    @Test
    @DisplayName("7. Invalid date rejects row")
    void testInvalidDateRejectsRow() {
        String line = "2026-99-99 UPI FreshMart Groceries Debit 1,845.50 Weekly groceries 22,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);
        assertNull(tx);
    }

    @Test
    @DisplayName("8. Missing amount rejects row")
    void testMissingAmountRejectsRow() {
        String line = "2026-10-09 UPI FreshMart Groceries Debit Weekly groceries 22,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);
        assertNull(tx);
    }

    @Test
    @DisplayName("9. Invalid debit/credit direction rejects row")
    void testInvalidDirectionRejectsRow() {
        String line = "2026-10-09 UPI FreshMart Groceries Pending 1,845.50 Weekly groceries 22,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);
        assertNull(tx);
    }

    @Test
    @DisplayName("10. Missing balance is safely tolerated as optional")
    void testMissingBalanceAccepted() {
        String line = "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Weekly household groceries";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(new BigDecimal("1845.50"), tx.getAmount());
        assertNull(tx.getBalance());
        assertEquals("FreshMart Supermarket - Weekly household groceries", tx.getDescription());
    }

    @Test
    @DisplayName("11. Malformed row rejects cleanly without exception")
    void testMalformedRow() {
        assertNull(parser.parsePositionalRow("", 1));
        assertNull(parser.parsePositionalRow("   ", 1));
        assertNull(parser.parsePositionalRow("Just random text without date", 1));
        assertNull(parser.parsePositionalRow("2026-10-09 not-a-channel", 1));
    }

    @Test
    @DisplayName("12. Reference numbers in narration do not confuse amount parser")
    void testReferenceNumbersInNarration() {
        String line = "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Ref 9876543210 order 54321 22,654.50";
        ParsedTransaction tx = parser.parsePositionalRow(line, 1);

        assertNotNull(tx);
        assertEquals(new BigDecimal("1845.50"), tx.getAmount());
        assertEquals(new BigDecimal("22654.50"), tx.getBalance());
        assertTrue(tx.getDescription().contains("Ref 9876543210 order 54321"));
    }

    @Test
    @DisplayName("13. Running balance mismatch produces warning without mutating amounts")
    void testRunningBalanceMismatchProducesWarning() {
        String statementText =
                "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,000.00 Groceries 20,000.00\n" +
                "2026-10-10 CARD Coffee Cafe Food Debit 500.00 Coffee 18,000.00\n"; // Expected 19,500.00, mismatch!

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());

        boolean hasDiscrepancyWarning = false;
        for (String w : result.getWarnings()) {
            if (w.contains("Running balance discrepancy")) {
                hasDiscrepancyWarning = true;
                break;
            }
        }
        assertTrue(hasDiscrepancyWarning, "Expected running balance discrepancy warning in: " + result.getWarnings());
        assertEquals(new BigDecimal("500.00"), result.getTransactions().get(1).getAmount());
    }

    @Test
    @DisplayName("14. Zero valid transactions returns PARSING_FAILURE with 0.00 confidence")
    void testZeroValidTransactionsReturnsParsingFailure() {
        String text = "Some header-like line without any transactions\nAnother random notes line\n";
        BigDecimal conf = parser.detectConfidence(text);
        assertEquals(new BigDecimal("0.00"), conf);

        StatementParseResult result = parser.parse(text);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getParsedRowCount());
    }

    @Test
    @DisplayName("15. Mixed valid and invalid rows parses valid rows and records unparsed lines")
    void testMixedValidAndInvalidRows() {
        String text =
                "2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Groceries 22,654.50\n" +
                "MALFORMED INVALID ROW THAT CANNOT BE PARSED\n" +
                "2026-10-10 CARD Fuel Station Transport Debit 2,500.00 Fuel 20,154.50\n";

        StatementParseResult result = parser.parse(text);
        assertEquals(StatementParseResult.Status.PARTIALLY_PARSED, result.getStatus());
        assertEquals(2, result.getParsedRowCount());
        assertEquals(1, result.getRejectedRowCount());
        assertEquals("MALFORMED INVALID ROW THAT CANNOT BE PARSED", result.getRawUnparsedLines().get(0));
    }

    @Test
    @DisplayName("16. Confidence must not be high when zero rows parse")
    void testConfidenceZeroWhenNoRowsParse() {
        String text = "Date | Description | Amount | Type | Balance\n"; // Header only, no rows
        BigDecimal conf = parser.detectConfidence(text);
        assertEquals(new BigDecimal("0.00"), conf, "Confidence must be 0.00 when 0 valid rows parse");
    }
}

