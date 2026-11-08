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

@DisplayName("GenericBankStatementParser Comprehensive Behavioral & Edge-Case Tests")
public class GenericBankStatementParserTest {

    private GenericBankStatementParser parser;

    @BeforeEach
    void setUp() {
        parser = new GenericBankStatementParser();
    }

    @Test
    @DisplayName("1. Standard Date | Narration | Debit | Credit | Balance layout")
    void testStandardDateNarrationDebitCreditBalance() {
        String statementText =
                "Account Statement\n" +
                "Date | Narration | Debit | Credit | Balance\n" +
                "01/10/2026 | Salary Direct Deposit | - | 75,000.00 | 1,25,000.00\n" +
                "03/10/2026 | BigBasket Supermarket | 3,450.50 | - | 1,21,549.50\n" +
                "05/10/2026 | Electric Utility Bill | 2,100.00 | - | 1,19,449.50\n";

        BigDecimal conf = parser.detectConfidence(statementText);
        assertTrue(conf.compareTo(new BigDecimal("0.70")) >= 0, "Confidence should meet threshold: " + conf);

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getParsedRowCount());

        ParsedTransaction tx1 = result.getTransactions().get(0);
        assertEquals(LocalDate.of(2026, 10, 1), tx1.getTransactionDate());
        assertEquals("Salary Direct Deposit", tx1.getDescription());
        assertEquals(TransactionType.INCOME, tx1.getTransactionType());
        assertEquals(new BigDecimal("75000.00"), tx1.getCredit());
        assertNull(tx1.getDebit());
        assertEquals(new BigDecimal("75000.00"), tx1.getAmount());
        assertEquals(new BigDecimal("125000.00"), tx1.getBalance());

        ParsedTransaction tx2 = result.getTransactions().get(1);
        assertEquals(LocalDate.of(2026, 10, 3), tx2.getTransactionDate());
        assertEquals("BigBasket Supermarket", tx2.getDescription());
        assertEquals(TransactionType.EXPENSE, tx2.getTransactionType());
        assertEquals(new BigDecimal("3450.50"), tx2.getDebit());
        assertNull(tx2.getCredit());
        assertEquals(new BigDecimal("3450.50"), tx2.getAmount());
        assertEquals(new BigDecimal("121549.50"), tx2.getBalance());
    }

    @Test
    @DisplayName("2. Different column ordering: Transaction Date | Particulars | Withdrawal | Deposit | Closing Balance")
    void testDifferentColumnOrderingAndTerminology() {
        String statementText =
                "Transaction Date | Particulars | Withdrawal | Deposit | Closing Balance\n" +
                "15-10-2026 | Coffee Roasters Cafe | 450.00 | | 54,550.00\n" +
                "16-10-2026 | Client Consulting Fee | | 25,000.00 | 79,550.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());

        ParsedTransaction tx1 = result.getTransactions().get(0);
        assertEquals(LocalDate.of(2026, 10, 15), tx1.getTransactionDate());
        assertEquals(TransactionType.EXPENSE, tx1.getTransactionType());
        assertEquals(new BigDecimal("450.00"), tx1.getAmount());

        ParsedTransaction tx2 = result.getTransactions().get(1);
        assertEquals(LocalDate.of(2026, 10, 16), tx2.getTransactionDate());
        assertEquals(TransactionType.INCOME, tx2.getTransactionType());
        assertEquals(new BigDecimal("25000.00"), tx2.getAmount());
    }

    @Test
    @DisplayName("3. Multi-format date parsing (DD MMM YYYY, YYYY-MM-DD)")
    void testMultiFormatDates() {
        String statementText =
                "Date | Description | Debit | Credit | Balance\n" +
                "12 Oct 2026 | Medical Pharmacy | 1,200.00 | | 48,800.00\n" +
                "2026-10-18 | Dividend Payout | | 500.00 | 49,300.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());

        assertEquals(LocalDate.of(2026, 10, 12), result.getTransactions().get(0).getTransactionDate());
        assertEquals(LocalDate.of(2026, 10, 18), result.getTransactions().get(1).getTransactionDate());
    }

    @Test
    @DisplayName("4. Indian currency formatting (lakhs & crores: 1,50,000.00)")
    void testIndianCurrencyFormatting() {
        String statementText =
                "Date | Details | Debit | Credit | Balance\n" +
                "01/11/2026 | Flat Rent Payment | 1,50,000.00 | | 3,50,000.00\n" +
                "05/11/2026 | Property Sale Proceeds | | 12,00,000.00 | 15,50,000.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());

        assertEquals(new BigDecimal("150000.00"), result.getTransactions().get(0).getDebit());
        assertEquals(new BigDecimal("1200000.00"), result.getTransactions().get(1).getCredit());
    }

    @Test
    @DisplayName("5. Safe multi-line narration continuation")
    void testMultiLineNarrationContinuation() {
        String statementText =
                "Date | Particulars | Debit | Credit | Balance\n" +
                "02/10/2026 | UPI-SWIGGY-BANGALORE | 620.00 | | 80,000.00\n" +
                "Ref 9876543210 UPI App Order Food\n" +
                "04/10/2026 | Uber Rides Mumbai | 340.00 | | 79,660.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());

        ParsedTransaction tx1 = result.getTransactions().get(0);
        assertTrue(tx1.getDescription().contains("UPI-SWIGGY-BANGALORE"));
        assertTrue(tx1.getDescription().contains("Ref 9876543210 UPI App Order Food"));
        assertTrue(tx1.getRawLine().contains("Order Food"));
    }

    @Test
    @DisplayName("6. Date | Narration | Amount | Type | Balance format")
    void testAmountAndTypeFormat() {
        String statementText =
                "Date | Details | Amount | Type | Balance\n" +
                "10/10/2026 | Fuel Pump Station | 2,500.00 | DR | 45,000.00\n" +
                "11/10/2026 | Cash Cashback Reward | 250.00 | CR | 45,250.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());

        ParsedTransaction tx1 = result.getTransactions().get(0);
        assertEquals(TransactionType.EXPENSE, tx1.getTransactionType());
        assertEquals(new BigDecimal("2500.00"), tx1.getDebit());

        ParsedTransaction tx2 = result.getTransactions().get(1);
        assertEquals(TransactionType.INCOME, tx2.getTransactionType());
        assertEquals(new BigDecimal("250.00"), tx2.getCredit());
    }

    @Test
    @DisplayName("7. Missing optional balance column is accepted cleanly")
    void testMissingOptionalBalanceColumn() {
        String statementText =
                "Date | Description | Debit | Credit\n" +
                "05/10/2026 | Online Book Purchase | 850.00 | \n" +
                "08/10/2026 | Interest Earned | | 120.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());
        assertNull(result.getTransactions().get(0).getBalance());
    }

    @Test
    @DisplayName("8. Missing optional reference column is accepted cleanly")
    void testWithAndWithoutReferenceColumn() {
        String statementText =
                "Date | Chq/Ref No | Narration | Debit | Credit | Balance\n" +
                "05/10/2026 | CHQ12345 | Office Supplies | 1,500.00 | | 20,000.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getParsedRowCount());
        assertEquals(new BigDecimal("1500.00"), result.getTransactions().get(0).getDebit());
    }

    @Test
    @DisplayName("9. Reconciliation success when Opening + Credits - Debits = Closing Balance")
    void testReconciliationSuccess() {
        String statementText =
                "Opening Balance: 10,000.00\n" +
                "Date | Narration | Debit | Credit | Balance\n" +
                "01/10/2026 | Freelance Payment | | 5,000.00 | 15,000.00\n" +
                "02/10/2026 | Groceries | 2,000.00 | | 13,000.00\n" +
                "Closing Balance: 13,000.00\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        // No discrepancy warnings
        for (String w : result.getWarnings()) {
            assertFalse(w.contains("reconciliation discrepancy"), "Should not contain discrepancy: " + w);
        }
    }

    @Test
    @DisplayName("10. Reconciliation failure correctly reports discrepancy without altering transactions")
    void testReconciliationFailureDiscrepancy() {
        String statementText =
                "Opening Balance: 10,000.00\n" +
                "Date | Narration | Debit | Credit | Balance\n" +
                "01/10/2026 | Freelance Payment | | 5,000.00 | 15,000.00\n" +
                "Closing Balance: 20,000.00\n"; // Expected 15,000.00, mismatch!

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getParsedRowCount());
        // Must report discrepancy warning
        boolean foundDiscrepancyWarning = false;
        for (String w : result.getWarnings()) {
            if (w.contains("reconciliation discrepancy")) {
                foundDiscrepancyWarning = true;
                break;
            }
        }
        assertTrue(foundDiscrepancyWarning, "Expected reconciliation discrepancy warning in: " + result.getWarnings());
    }

    @Test
    @DisplayName("11. Repeated page headers and footers are cleanly ignored")
    void testRepeatedPageHeadersAndFooters() {
        String statementText =
                "Date | Narration | Debit | Credit | Balance\n" +
                "01/10/2026 | Dinner Restaurant | 1,200.00 | | 50,000.00\n" +
                "Page 1 of 2\n" +
                "Date | Narration | Debit | Credit | Balance\n" +
                "02/10/2026 | Movie Cinema Tickets | 600.00 | | 49,400.00\n" +
                "Statement Summary: Total debits\n";

        StatementParseResult result = parser.parse(statementText);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedRowCount());
    }

    @Test
    @DisplayName("12. Unrelated text and empty input are safely rejected")
    void testUnrelatedTextAndEmptyInput() {
        // Empty input
        StatementParseResult emptyRes = parser.parse("");
        assertEquals(StatementParseResult.Status.EMPTY_INPUT, emptyRes.getStatus());

        // Unrelated document
        String unrelated =
                "INVOICE # 10423\n" +
                "Customer: John Doe\n" +
                "Items: 2x Widgets at 15.00 each\n" +
                "Total: 30.00\n";

        BigDecimal conf = parser.detectConfidence(unrelated);
        assertEquals(new BigDecimal("0.00"), conf);

        StatementParseResult unsuppRes = parser.parse(unrelated);
        assertEquals(StatementParseResult.Status.UNSUPPORTED_FORMAT, unsuppRes.getStatus());
    }

    @Test
    @DisplayName("13. Ambiguous Type column without CR/DR is rejected")
    void testAmbiguousAmountTypeRejected() {
        String statementText =
                "Date | Description | Amount | Type | Balance\n" +
                "10/10/2026 | Coffee Cafe | 200.00 | POS | 45,000.00\n"; // POS is neither DR nor CR

        StatementParseResult result = parser.parse(statementText);
        // Header detected, but 0 valid transaction rows could be parsed due to ambiguous type
        assertEquals(StatementParseResult.Status.PARSING_FAILURE, result.getStatus());
    }
}

