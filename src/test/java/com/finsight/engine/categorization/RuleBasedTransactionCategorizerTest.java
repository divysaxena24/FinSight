package com.finsight.engine.categorization;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuleBasedTransactionCategorizer Unit Tests")
public class RuleBasedTransactionCategorizerTest {

    private RuleBasedTransactionCategorizer categorizer;

    @BeforeEach
    void setUp() {
        categorizer = new RuleBasedTransactionCategorizer();
    }

    private ParsedTransaction createTx(String desc, BigDecimal amount, TransactionType type) {
        return new ParsedTransaction(
                "tx-1", LocalDate.of(2026, 3, 1), desc,
                type == TransactionType.EXPENSE ? amount : null,
                type == TransactionType.INCOME ? amount : null,
                amount, type, null, null, null,
                BigDecimal.ONE, desc, "TEST_BANK",
                false, false, false, false, false
        );
    }

    @Test
    @DisplayName("1. Salary credit categorization with high confidence (>= 0.85)")
    void testSalaryCredit() {
        ParsedTransaction tx = createTx("OCTOBER MONTHLY SALARY CREDIT BY EMPLOYER", new BigDecimal("75000.00"), TransactionType.INCOME);
        CategorizationResult result = categorizer.categorize(tx);

        assertEquals("Salary", result.getCategory());
        assertTrue(result.getConfidence().compareTo(new BigDecimal("0.85")) >= 0);
        assertFalse(result.isNeedsUserReview());
        assertFalse(result.isTransfer());
    }

    @Test
    @DisplayName("2. Food & Dining merchant and keyword categorization")
    void testFoodAndDining() {
        ParsedTransaction txSwiggy = createTx("SWIGGY*ORDER12345 BANGALORE", new BigDecimal("450.00"), TransactionType.EXPENSE);
        CategorizationResult resSwiggy = categorizer.categorize(txSwiggy);
        assertEquals("Food & Dining", resSwiggy.getCategory());
        assertEquals("Swiggy", resSwiggy.getNormalizedMerchant());
        assertEquals(new BigDecimal("0.95"), resSwiggy.getConfidence());

        ParsedTransaction txCafe = createTx("BLUE TOKAI COFFEE ROASTERS", new BigDecimal("350.00"), TransactionType.EXPENSE);
        CategorizationResult resCafe = categorizer.categorize(txCafe);
        assertEquals("Food & Dining", resCafe.getCategory());
    }

    @Test
    @DisplayName("3. Shopping merchant and retail store categorization")
    void testShopping() {
        ParsedTransaction txAmazon = createTx("AMZN PAYMENTS INDIA PVT LTD", new BigDecimal("2500.00"), TransactionType.EXPENSE);
        CategorizationResult resAmazon = categorizer.categorize(txAmazon);
        assertEquals("Shopping", resAmazon.getCategory());
        assertEquals("Amazon", resAmazon.getNormalizedMerchant());

        ParsedTransaction txZara = createTx("ZARA RETAIL APPAREL PHOENIX", new BigDecimal("5000.00"), TransactionType.EXPENSE);
        CategorizationResult resZara = categorizer.categorize(txZara);
        assertEquals("Shopping", resZara.getCategory());
        assertEquals("Zara", resZara.getNormalizedMerchant());
    }

    @Test
    @DisplayName("4. Transport, fuel and flight categorization")
    void testTransport() {
        ParsedTransaction txUber = createTx("UBER INDIA RIDES BANGALORE", new BigDecimal("320.00"), TransactionType.EXPENSE);
        CategorizationResult resUber = categorizer.categorize(txUber);
        assertEquals("Transport", resUber.getCategory());
        assertEquals("Uber", resUber.getNormalizedMerchant());

        ParsedTransaction txFuel = createTx("INDIAN OIL PETROL PUMP", new BigDecimal("2000.00"), TransactionType.EXPENSE);
        CategorizationResult resFuel = categorizer.categorize(txFuel);
        assertEquals("Transport", resFuel.getCategory());
        assertEquals("Fuel Station", resFuel.getNormalizedMerchant());
    }

    @Test
    @DisplayName("5. Housing & Rent categorization")
    void testHousingAndRent() {
        ParsedTransaction txRent = createTx("APARTMENT RENT FOR MARCH PAID TO LANDLORD", new BigDecimal("22000.00"), TransactionType.EXPENSE);
        CategorizationResult resRent = categorizer.categorize(txRent);
        assertEquals("Housing & Rent", resRent.getCategory());
        assertEquals(new BigDecimal("0.90"), resRent.getConfidence());
    }

    @Test
    @DisplayName("6. Utilities & Telecom categorization")
    void testUtilities() {
        ParsedTransaction txElec = createTx("BESCOM ELECTRICITY BILL PAYMENT", new BigDecimal("1450.00"), TransactionType.EXPENSE);
        CategorizationResult resElec = categorizer.categorize(txElec);
        assertEquals("Utilities", resElec.getCategory());
        assertEquals("BESCOM Electricity", resElec.getNormalizedMerchant());

        ParsedTransaction txAirtel = createTx("AIRTEL BROADBAND FIBER", new BigDecimal("999.00"), TransactionType.EXPENSE);
        CategorizationResult resAirtel = categorizer.categorize(txAirtel);
        assertEquals("Utilities", resAirtel.getCategory());
        assertEquals("Airtel", resAirtel.getNormalizedMerchant());
    }

    @Test
    @DisplayName("7. Healthcare & Medical categorization")
    void testHealthcare() {
        ParsedTransaction txApollo = createTx("APOLLO PHARMACY INDIRANAGAR", new BigDecimal("650.00"), TransactionType.EXPENSE);
        CategorizationResult resApollo = categorizer.categorize(txApollo);
        assertEquals("Healthcare", resApollo.getCategory());
        assertEquals("Apollo Healthcare", resApollo.getNormalizedMerchant());
    }

    @Test
    @DisplayName("8. Entertainment & Subscriptions categorization")
    void testEntertainment() {
        ParsedTransaction txNetflix = createTx("NETFLIX ENTERTAINMENT SERVICES", new BigDecimal("649.00"), TransactionType.EXPENSE);
        CategorizationResult resNetflix = categorizer.categorize(txNetflix);
        assertEquals("Entertainment", resNetflix.getCategory());
        assertEquals("Netflix", resNetflix.getNormalizedMerchant());
    }

    @Test
    @DisplayName("9. Education categorization")
    void testEducation() {
        ParsedTransaction txEdu = createTx("UDEMY ONLINE COURSE CERTIFICATION", new BigDecimal("599.00"), TransactionType.EXPENSE);
        CategorizationResult resEdu = categorizer.categorize(txEdu);
        assertEquals("Education", resEdu.getCategory());
    }

    @Test
    @DisplayName("10. Investment categorization")
    void testInvestment() {
        ParsedTransaction txInvest = createTx("ZERODHA BROKING LTD MUTUAL FUND SIP", new BigDecimal("10000.00"), TransactionType.EXPENSE);
        CategorizationResult resInvest = categorizer.categorize(txInvest);
        assertEquals("Investment", resInvest.getCategory());
        assertEquals("Zerodha", resInvest.getNormalizedMerchant());
    }

    @Test
    @DisplayName("11. EMI & Loans categorization")
    void testEmiAndLoans() {
        ParsedTransaction txEmi = createTx("HDFC AUTO LOAN EMI DEBIT", new BigDecimal("14250.00"), TransactionType.EXPENSE);
        CategorizationResult resEmi = categorizer.categorize(txEmi);
        assertEquals("EMI & Loans", resEmi.getCategory());
    }

    @Test
    @DisplayName("12. Own-account transfers vs merchant UPI distinction")
    void testTransfersVsMerchantUpi() {
        // Own account sweep/transfer is recognized as Transfers
        ParsedTransaction txOwn = createTx("TRF TO SELF SAVINGS ACCOUNT", new BigDecimal("5000.00"), TransactionType.EXPENSE);
        CategorizationResult resOwn = categorizer.categorize(txOwn);
        assertEquals("Transfers", resOwn.getCategory());
        assertTrue(resOwn.isTransfer());

        // Merchant UPI (e.g. Swiggy through UPI) is classified as Food & Dining, NOT a transfer!
        ParsedTransaction txUpiMerchant = createTx("UPI/308115629124/SWIGGY/BANGALORE", new BigDecimal("350.00"), TransactionType.EXPENSE);
        CategorizationResult resUpiMerchant = categorizer.categorize(txUpiMerchant);
        assertEquals("Food & Dining", resUpiMerchant.getCategory());
        assertEquals("Swiggy", resUpiMerchant.getNormalizedMerchant());
        assertFalse(resUpiMerchant.isTransfer());
    }

    @Test
    @DisplayName("13. Refunds & reversals are flagged and attributed correctly")
    void testRefundsAndReversals() {
        // Swiggy refund -> Food & Dining, isRefund = true
        ParsedTransaction txSwiggyRefund = createTx("SWIGGY*ORDER12345 REFUND", new BigDecimal("450.00"), TransactionType.INCOME);
        CategorizationResult resSwiggy = categorizer.categorize(txSwiggyRefund);
        assertEquals("Food & Dining", resSwiggy.getCategory());
        assertEquals("Swiggy", resSwiggy.getNormalizedMerchant());
        assertTrue(resSwiggy.isRefundOrReversal());
        assertFalse(resSwiggy.isNeedsUserReview());

        // Amazon refund -> Shopping, isRefund = true
        ParsedTransaction txAmazonRefund = createTx("AMAZON RETURN REFUND", new BigDecimal("1200.00"), TransactionType.INCOME);
        CategorizationResult resAmazon = categorizer.categorize(txAmazonRefund);
        assertEquals("Shopping", resAmazon.getCategory());
        assertEquals("Amazon", resAmazon.getNormalizedMerchant());
        assertTrue(resAmazon.isRefundOrReversal());
        assertFalse(resAmazon.isNeedsUserReview());

        // Refund with no identifiable merchant -> category "Other", isRefund = true, needsUserReview = true
        ParsedTransaction txNoMerchant = createTx("REFUND TXN 99201", new BigDecimal("250.00"), TransactionType.INCOME);
        CategorizationResult resNoMerchant = categorizer.categorize(txNoMerchant);
        assertEquals("Other", resNoMerchant.getCategory());
        assertTrue(resNoMerchant.isRefundOrReversal());
        assertTrue(resNoMerchant.isNeedsUserReview());

        // Reversal with existing known category -> preserves existing category, isRefund = true
        ParsedTransaction txReversalWithCat = createTx("REV-BILL PAY", new BigDecimal("999.00"), TransactionType.INCOME);
        txReversalWithCat.setCategory("Utilities");
        CategorizationResult resReversal = categorizer.categorize(txReversalWithCat);
        assertEquals("Utilities", resReversal.getCategory());
        assertTrue(resReversal.isRefundOrReversal());

        // Refund with no original purchase reference -> fallback "Other", needsUserReview = true
        ParsedTransaction txUnreferenced = createTx("CASHBACK REVERSAL 100.00", new BigDecimal("100.00"), TransactionType.INCOME);
        CategorizationResult resUnref = categorizer.categorize(txUnreferenced);
        assertEquals("Other", resUnref.getCategory());
        assertTrue(resUnref.isRefundOrReversal());
        assertTrue(resUnref.isNeedsUserReview());
    }

    @Test
    @DisplayName("13b. Three-tier confidence policy enforcement")
    void testThreeTierConfidencePolicy() {
        // High confidence (>= 0.85) -> matched category, needsUserReview = false
        ParsedTransaction txHigh = createTx("OCTOBER SALARY CREDIT", new BigDecimal("80000.00"), TransactionType.INCOME);
        CategorizationResult resHigh = categorizer.categorize(txHigh);
        assertTrue(resHigh.getConfidence().compareTo(new BigDecimal("0.85")) >= 0);
        assertFalse(resHigh.isNeedsUserReview());

        // Medium confidence (>= 0.70 and < 0.85) -> matched category, needsUserReview = true
        // Rule SHOP_KEYWORDS has confidence 0.75
        ParsedTransaction txMed = createTx("LOCAL SUPERMARKET MART PURCHASE", new BigDecimal("1500.00"), TransactionType.EXPENSE);
        CategorizationResult resMed = categorizer.categorize(txMed);
        assertEquals("Shopping", resMed.getCategory());
        assertEquals(new BigDecimal("0.75"), resMed.getConfidence());
        assertTrue(resMed.isNeedsUserReview());

        // Low confidence (< 0.70) -> category "Other", needsUserReview = true
        ParsedTransaction txLow = createTx("RANDOM UNKNOWN STRING 123", new BigDecimal("42.00"), TransactionType.EXPENSE);
        CategorizationResult resLow = categorizer.categorize(txLow);
        assertEquals("Other", resLow.getCategory());
        assertTrue(resLow.getConfidence().compareTo(new BigDecimal("0.70")) < 0);
        assertTrue(resLow.isNeedsUserReview());
    }

    @Test
    @DisplayName("14. Ambiguous or unknown narration assigns category 'Other' and flags user review")
    void testUnknownNarration() {
        ParsedTransaction txUnknown = createTx("MISC FEE CHG ADJ 99401", new BigDecimal("100.00"), TransactionType.EXPENSE);
        CategorizationResult resUnknown = categorizer.categorize(txUnknown);

        assertEquals("Other", resUnknown.getCategory());
        assertTrue(resUnknown.getConfidence().compareTo(new BigDecimal("0.70")) < 0);
        assertTrue(resUnknown.isNeedsUserReview());
    }

    @Test
    @DisplayName("15. Manual category correction preservation: automatic rules never overwrite user edits")
    void testManualCorrectionPreservation() {
        ParsedTransaction tx = createTx("SWIGGY*ORDER12345", new BigDecimal("450.00"), TransactionType.EXPENSE);
        // User manually classified this business meal as "Entertainment"
        tx.setCategory("Entertainment");
        tx.setManuallyCategorized(true);

        categorizer.applyCategorization(tx);

        // Verification: Existing category "Entertainment" is preserved, NOT overwritten to "Food & Dining"
        assertEquals("Entertainment", tx.getCategory());
        assertTrue(tx.isManuallyCategorized());
    }

    @Test
    @DisplayName("16. Deterministic execution: identical input produces identical category output")
    void testDeterministicExecution() {
        ParsedTransaction tx1 = createTx("UBER INDIA RIDES", new BigDecimal("250.00"), TransactionType.EXPENSE);
        ParsedTransaction tx2 = createTx("UBER INDIA RIDES", new BigDecimal("250.00"), TransactionType.EXPENSE);

        CategorizationResult r1 = categorizer.categorize(tx1);
        CategorizationResult r2 = categorizer.categorize(tx2);

        assertEquals(r1.getCategory(), r2.getCategory());
        assertEquals(r1.getNormalizedMerchant(), r2.getNormalizedMerchant());
        assertEquals(r1.getConfidence(), r2.getConfidence());
        assertEquals(r1.getMatchedRule(), r2.getMatchedRule());
    }
}

