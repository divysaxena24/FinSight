package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.insights.FinancialInsight.InsightType;
import com.finsight.engine.insights.FinancialInsight.Severity;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Historical Spending Intelligence & Budget Recommendations Tests")
public class SpendingIntelligenceTest {

    private SpendingBaselineCalculator baselineCalculator;
    private UnusualTransactionDetector detector;
    private BudgetRecommendationEngine budgetEngine;

    @BeforeEach
    void setUp() {
        baselineCalculator = new SpendingBaselineCalculator(2);
        detector = new UnusualTransactionDetector(baselineCalculator);
        budgetEngine = new BudgetRecommendationEngine(baselineCalculator, new BigDecimal("10.00"));
    }

    private ParsedTransaction createExpense(String id, LocalDate date, String desc, BigDecimal amt, String cat, String merch) {
        return new ParsedTransaction(
                id, date, desc, amt, null, amt, TransactionType.EXPENSE, null,
                merch, cat, BigDecimal.ONE, desc, "STATEMENT",
                false, false, false, false
        );
    }

    private ParsedTransaction createIncome(String id, LocalDate date, String desc, BigDecimal amt, boolean isRefund, String cat, String merch) {
        return new ParsedTransaction(
                id, date, desc, null, amt, amt, TransactionType.INCOME, null,
                merch, cat, BigDecimal.ONE, desc, "STATEMENT",
                false, false, false, isRefund
        );
    }

    private ParsedTransaction createTransfer(String id, LocalDate date, String desc, BigDecimal amt) {
        return new ParsedTransaction(
                id, date, desc, amt, null, amt, TransactionType.TRANSFER, null,
                "Self", "Transfers", BigDecimal.ONE, desc, "STATEMENT",
                false, false, false, false
        );
    }

    @Test
    @DisplayName("1. Unusually large expense detection via category median rule")
    void testUnusuallyLargeExpenseDetection() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Food & Dining transactions: 200, 250, 300, 350 -> median = 275.
        // A 3500.00 dinner is ~12.7x median, well above 2.5x threshold!
        txs.add(createExpense("t1", LocalDate.of(2026, 1, 5), "Dinner Cafe", new BigDecimal("200.00"), "Food & Dining", "Cafe"));
        txs.add(createExpense("t2", LocalDate.of(2026, 1, 10), "Lunch Place", new BigDecimal("250.00"), "Food & Dining", "Restaurant"));
        txs.add(createExpense("t3", LocalDate.of(2026, 1, 15), "Dinner Buffet", new BigDecimal("300.00"), "Food & Dining", "Buffet"));
        txs.add(createExpense("t4", LocalDate.of(2026, 1, 20), "Bistro Order", new BigDecimal("350.00"), "Food & Dining", "Bistro"));
        txs.add(createExpense("t-large", LocalDate.of(2026, 1, 25), "Lavish Banquet Feast", new BigDecimal("3500.00"), "Food & Dining", "Luxury Hotel"));

        List<FinancialInsight> insights = detector.detectUnusuallyLargeExpenses(txs, null, null);
        assertEquals(1, insights.size());

        FinancialInsight ins = insights.get(0);
        assertEquals(InsightType.UNUSUAL_EXPENSE, ins.getType());
        assertEquals(Severity.WARNING, ins.getSeverity());
        assertEquals("Food & Dining", ins.getCategory());
        assertEquals(new BigDecimal("3500.00"), ins.getAmount());
        assertTrue(ins.getSupportingTransactionIds().contains("t-large"));
        assertTrue(ins.getCalculationEvidence().contains("Median"));
    }

    @Test
    @DisplayName("2. Category spending spike detection across historical periods")
    void testCategorySpendingSpike() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Month 1 (Jan 2026): Shopping spend = 2000.00
        txs.add(createExpense("s1", LocalDate.of(2026, 1, 10), "Amazon Retail", new BigDecimal("2000.00"), "Shopping", "Amazon"));
        // Month 2 (Feb 2026): Shopping spend = 2200.00 (historical baseline ~ 2100.00)
        txs.add(createExpense("s2", LocalDate.of(2026, 2, 10), "Flipkart Electronics", new BigDecimal("2200.00"), "Shopping", "Flipkart"));
        // Month 3 (Mar 2026): Shopping spike = 8000.00 (spiked by ~280% above baseline)
        txs.add(createExpense("s3", LocalDate.of(2026, 3, 15), "Luxury Designer Watch", new BigDecimal("8000.00"), "Shopping", "Watch Store"));

        List<FinancialInsight> spikes = detector.detectCategorySpikes(txs, null, null);
        assertEquals(1, spikes.size());

        FinancialInsight spike = spikes.get(0);
        assertEquals(InsightType.CATEGORY_SPENDING_SPIKE, spike.getType());
        assertEquals(Severity.ALERT, spike.getSeverity());
        assertEquals("Shopping", spike.getCategory());
        assertEquals(new BigDecimal("8000.00"), spike.getAmount());
        assertTrue(spike.getCalculationEvidence().contains("Historical Baseline"));
    }

    @Test
    @DisplayName("3. Insufficient historical data handling")
    void testInsufficientHistoricalData() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Only 1 single month of Healthcare spending
        txs.add(createExpense("h1", LocalDate.of(2026, 3, 5), "Pharmacy", new BigDecimal("450.00"), "Healthcare", "Apollo Healthcare"));

        // Baseline calculator requires 2 months by default
        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, null, null);
        assertFalse(baselines.containsKey("Healthcare"));

        // Budget recommendation engine should decline to recommend without evidence
        List<BudgetRecommendation> budgets = budgetEngine.generateRecommendations(txs, null, null);
        assertEquals(0, budgets.size());
    }

    @Test
    @DisplayName("4. Suspected vs confirmed duplicates handling")
    void testSuspectedVsConfirmedDuplicates() {
        List<ParsedTransaction> txs = new ArrayList<>();
        ParsedTransaction t1 = createExpense("t1", LocalDate.of(2026, 3, 10), "Uber India Rides", new BigDecimal("420.00"), "Transport", "Uber");
        ParsedTransaction t2 = createExpense("t2", LocalDate.of(2026, 3, 10), "Uber India Rides", new BigDecimal("420.00"), "Transport", "Uber");
        t2.setDuplicateCandidate(true); // Suspected duplicate

        txs.add(t1);
        txs.add(t2);

        // Suspected duplicate is detected and flagged as insight
        List<FinancialInsight> dupInsights = detector.detectPotentialDuplicates(txs, null, null);
        assertEquals(1, dupInsights.size());
        assertEquals(InsightType.POTENTIAL_DUPLICATE, dupInsights.get(0).getType());
        assertTrue(dupInsights.get(0).getSupportingTransactionIds().contains("t1"));
        assertTrue(dupInsights.get(0).getSupportingTransactionIds().contains("t2"));

        // Suspected duplicate is NOT silently excluded from monthly spending calculations!
        Map<String, BigDecimal> baselineSuspected = baselineCalculator.calculateCategoryBaselines(
                List.of(
                        createExpense("t0", LocalDate.of(2026, 2, 1), "Uber Ride", new BigDecimal("400.00"), "Transport", "Uber"),
                        t1, t2
                ), null, null);
        // March spend = 420 + 420 = 840; Feb spend = 400. Baseline = (400 + 840) / 2 = 620.00
        assertEquals(new BigDecimal("620.00"), baselineSuspected.get("Transport"));

        // When confirmed as duplicate, it IS excluded from baseline
        t2.setConfirmedDuplicate(true);
        Map<String, BigDecimal> baselineConfirmed = baselineCalculator.calculateCategoryBaselines(
                List.of(
                        createExpense("t0", LocalDate.of(2026, 2, 1), "Uber Ride", new BigDecimal("400.00"), "Transport", "Uber"),
                        t1, t2
                ), null, null);
        // March spend = 420; Feb spend = 400. Baseline = (400 + 420) / 2 = 410.00
        assertEquals(new BigDecimal("410.00"), baselineConfirmed.get("Transport"));
    }

    @Test
    @DisplayName("5. Refund and reversal treatment consistently reduces net baseline")
    void testRefundAndReversalTreatment() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Month 1: 3000.00 shopping
        txs.add(createExpense("m1", LocalDate.of(2026, 1, 15), "Amazon Purchase", new BigDecimal("3000.00"), "Shopping", "Amazon"));
        // Month 2: 3000.00 shopping - 1000.00 Amazon return refund = 2000.00 net
        txs.add(createExpense("m2-exp", LocalDate.of(2026, 2, 10), "Amazon Purchase", new BigDecimal("3000.00"), "Shopping", "Amazon"));
        txs.add(createIncome("m2-ref", LocalDate.of(2026, 2, 15), "Amazon Return Refund", new BigDecimal("1000.00"), true, "Shopping", "Amazon"));

        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, null, null);
        // Month 1 net = 3000.00, Month 2 net = 2000.00. Average baseline = (3000 + 2000) / 2 = 2500.00
        assertEquals(new BigDecimal("2500.00"), baselines.get("Shopping"));
    }

    @Test
    @DisplayName("6. Recurring monthly payment candidate detection (~30 day interval)")
    void testRecurringMonthlyPaymentDetection() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createExpense("n1", LocalDate.of(2026, 1, 5), "Netflix Subscription", new BigDecimal("649.00"), "Entertainment", "Netflix"));
        txs.add(createExpense("n2", LocalDate.of(2026, 2, 4), "Netflix Subscription", new BigDecimal("649.00"), "Entertainment", "Netflix")); // 30 days
        txs.add(createExpense("n3", LocalDate.of(2026, 3, 6), "Netflix Subscription", new BigDecimal("649.00"), "Entertainment", "Netflix")); // 30 days

        List<FinancialInsight> recurring = detector.detectRecurringPayments(txs, null, null);
        assertEquals(1, recurring.size());

        FinancialInsight ins = recurring.get(0);
        assertEquals(InsightType.RECURRING_PAYMENT, ins.getType());
        assertEquals("Netflix", ins.getMerchant());
        assertEquals(new BigDecimal("649.00"), ins.getAmount());
        assertEquals(3, ins.getSupportingTransactionIds().size());
        assertTrue(ins.getTitle().contains("Monthly"));
    }

    @Test
    @DisplayName("7. Recurring weekly payment candidate detection (~7 day interval)")
    void testRecurringWeeklyPaymentDetection() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createExpense("w1", LocalDate.of(2026, 1, 2), "Fresh Organic Farm Milk", new BigDecimal("350.00"), "Food & Dining", "Farm Milk"));
        txs.add(createExpense("w2", LocalDate.of(2026, 1, 9), "Fresh Organic Farm Milk", new BigDecimal("350.00"), "Food & Dining", "Farm Milk"));
        txs.add(createExpense("w3", LocalDate.of(2026, 1, 16), "Fresh Organic Farm Milk", new BigDecimal("350.00"), "Food & Dining", "Farm Milk"));
        txs.add(createExpense("w4", LocalDate.of(2026, 1, 23), "Fresh Organic Farm Milk", new BigDecimal("350.00"), "Food & Dining", "Farm Milk"));

        List<FinancialInsight> recurring = detector.detectRecurringPayments(txs, null, null);
        assertEquals(1, recurring.size());

        FinancialInsight ins = recurring.get(0);
        assertEquals(InsightType.RECURRING_PAYMENT, ins.getType());
        assertTrue(ins.getTitle().contains("Weekly"));
        assertEquals(4, ins.getSupportingTransactionIds().size());
    }

    @Test
    @DisplayName("8. Budget recommendation calculation with configurable safety margin")
    void testBudgetRecommendationWithSafetyMargin() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Utilities: Jan = 1500.00, Feb = 1500.00. Average baseline = 1500.00
        txs.add(createExpense("u1", LocalDate.of(2026, 1, 10), "Electricity BESCOM", new BigDecimal("1500.00"), "Utilities", "BESCOM"));
        txs.add(createExpense("u2", LocalDate.of(2026, 2, 10), "Electricity BESCOM", new BigDecimal("1500.00"), "Utilities", "BESCOM"));

        // Default 10% safety margin -> 1500 + 10% = 1650.00
        List<BudgetRecommendation> recs = budgetEngine.generateRecommendations(txs, null, null, new BigDecimal("10.00"));
        assertEquals(1, recs.size());

        BudgetRecommendation rec = recs.get(0);
        assertEquals("Utilities", rec.getCategory());
        assertEquals(new BigDecimal("1500.00"), rec.getObservedMonthlyBaseline());
        assertEquals(new BigDecimal("10.00"), rec.getSafetyMarginPercent());
        assertEquals(new BigDecimal("1650.00"), rec.getRecommendedBudget());
        assertFalse(rec.isSpendingTrendingUp());
        assertTrue(rec.getCalculationRationale().contains("1650.00"));
    }

    @Test
    @DisplayName("9. Upward spending trend detection adjusts safety buffer")
    void testUpwardSpendingTrendDetection() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Food: Jan = 4000.00, Feb = 5000.00, Mar = 6500.00 (Consistently rising spending)
        txs.add(createExpense("f1", LocalDate.of(2026, 1, 10), "Groceries", new BigDecimal("4000.00"), "Food & Dining", "Supermarket"));
        txs.add(createExpense("f2", LocalDate.of(2026, 2, 10), "Groceries", new BigDecimal("5000.00"), "Food & Dining", "Supermarket"));
        txs.add(createExpense("f3", LocalDate.of(2026, 3, 10), "Groceries", new BigDecimal("6500.00"), "Food & Dining", "Supermarket"));

        List<BudgetRecommendation> recs = budgetEngine.generateRecommendations(txs, null, null, new BigDecimal("10.00"));
        assertEquals(1, recs.size());

        BudgetRecommendation rec = recs.get(0);
        assertTrue(rec.isSpendingTrendingUp());
        // Base average = (4000 + 5000 + 6500) / 3 = 5166.67
        assertEquals(new BigDecimal("5166.67"), rec.getObservedMonthlyBaseline());
        // Upward trend buffer = 10% + 5% = 15% -> 5166.67 * 1.15 = 5941.67
        assertEquals(new BigDecimal("15.00"), rec.getSafetyMarginPercent());
        assertEquals(new BigDecimal("5941.67"), rec.getRecommendedBudget());
    }

    @Test
    @DisplayName("10. Transfer transactions are strictly excluded from baselines and budgets")
    void testTransferExclusion() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // 50,000 self sweep/transfer must never inflate monthly expense baselines!
        txs.add(createTransfer("trf1", LocalDate.of(2026, 1, 2), "Transfer to Savings", new BigDecimal("50000.00")));
        txs.add(createTransfer("trf2", LocalDate.of(2026, 2, 2), "Transfer to Savings", new BigDecimal("50000.00")));
        // Actual living expense: 1000.00 each month
        txs.add(createExpense("e1", LocalDate.of(2026, 1, 10), "Internet Bill", new BigDecimal("1000.00"), "Utilities", "ISP"));
        txs.add(createExpense("e2", LocalDate.of(2026, 2, 10), "Internet Bill", new BigDecimal("1000.00"), "Utilities", "ISP"));

        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, null, null);
        assertFalse(baselines.containsKey("Transfers"));
        assertEquals(new BigDecimal("1000.00"), baselines.get("Utilities"));
    }

    @Test
    @DisplayName("11. Out-of-period transactions boundary isolation")
    void testOutOfPeriodIsolation() {
        List<ParsedTransaction> txs = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        // Before declared period: Jan 15 (should be excluded)
        txs.add(createExpense("out1", LocalDate.of(2026, 1, 15), "Old Purchase", new BigDecimal("9999.00"), "Shopping", "Shop"));
        // In declared period: Feb & March
        txs.add(createExpense("in1", LocalDate.of(2026, 2, 15), "Feb Purchase", new BigDecimal("1000.00"), "Shopping", "Shop"));
        txs.add(createExpense("in2", LocalDate.of(2026, 3, 15), "Mar Purchase", new BigDecimal("1000.00"), "Shopping", "Shop"));
        // After declared period: April 10 (should be excluded)
        txs.add(createExpense("out2", LocalDate.of(2026, 4, 10), "Future Purchase", new BigDecimal("9999.00"), "Shopping", "Shop"));

        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, start, end);
        // Only Feb & March should be considered: average = (1000 + 1000) / 2 = 1000.00
        assertEquals(new BigDecimal("1000.00"), baselines.get("Shopping"));
    }

    @Test
    @DisplayName("12. Deterministic execution produces identical repeated results")
    void testDeterministicExecution() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createExpense("t1", LocalDate.of(2026, 1, 10), "Store", new BigDecimal("1200.00"), "Shopping", "Store"));
        txs.add(createExpense("t2", LocalDate.of(2026, 2, 10), "Store", new BigDecimal("1400.00"), "Shopping", "Store"));

        List<BudgetRecommendation> recs1 = budgetEngine.generateRecommendations(txs, null, null);
        List<BudgetRecommendation> recs2 = budgetEngine.generateRecommendations(txs, null, null);

        assertEquals(recs1.size(), recs2.size());
        assertEquals(recs1.get(0).getRecommendedBudget(), recs2.get(0).getRecommendedBudget());
        assertEquals(recs1.get(0).getCalculationRationale(), recs2.get(0).getCalculationRationale());
    }

    @Test
    @DisplayName("13. Zero floating-point drift and BigDecimal exact rounding")
    void testBigDecimalPrecisionAndRounding() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Months with fractional cents:
        // Jan: 33.33, Feb: 33.33, Mar: 33.34 -> Total = 100.00 -> Avg = 33.33
        txs.add(createExpense("c1", LocalDate.of(2026, 1, 1), "Subscription", new BigDecimal("33.33"), "Entertainment", "Sub"));
        txs.add(createExpense("c2", LocalDate.of(2026, 2, 1), "Subscription", new BigDecimal("33.33"), "Entertainment", "Sub"));
        txs.add(createExpense("c3", LocalDate.of(2026, 3, 1), "Subscription", new BigDecimal("33.34"), "Entertainment", "Sub"));

        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, null, null);
        assertEquals(new BigDecimal("33.33"), baselines.get("Entertainment"));

        // 10% safety margin -> 33.33 * 1.10 = 36.663 -> 36.66
        List<BudgetRecommendation> recs = budgetEngine.generateRecommendations(txs, null, null, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("36.66"), recs.get(0).getRecommendedBudget());
    }

    @Test
    @DisplayName("14. Zero-spending eligible months are included in monthly baseline averages")
    void testZeroSpendingEligibleMonthsIncluded() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Statement period covers full Q1: 2026-01-01 to 2026-03-31 (3 full months: Jan, Feb, Mar)
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        // Shopping in Jan: 3000.00, Shopping in Mar: 3000.00. February had 0.00 shopping.
        txs.add(createExpense("s1", LocalDate.of(2026, 1, 15), "Jan Shopping", new BigDecimal("3000.00"), "Shopping", "Store"));
        txs.add(createExpense("s3", LocalDate.of(2026, 3, 15), "Mar Shopping", new BigDecimal("3000.00"), "Shopping", "Store"));

        // With statement boundaries provided, denominator includes February (3 full months)
        // Average baseline = (3000 + 0 + 3000) / 3 = 2000.00 (not 3000.00!)
        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(txs, start, end);
        assertEquals(new BigDecimal("2000.00"), baselines.get("Shopping"));
    }

    @Test
    @DisplayName("15. Partial statement months do not dilute monthly baselines")
    void testPartialStatementMonthsHandledConsistently() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Statement starts mid-month: Jan 20 to March 31.
        // Full months are Feb and Mar. Jan is a partial month.
        LocalDate start = LocalDate.of(2026, 1, 20);
        LocalDate end = LocalDate.of(2026, 3, 31);

        List<java.time.YearMonth> fullMonths = baselineCalculator.getEligibleFullMonths(txs, start, end);
        assertEquals(2, fullMonths.size());
        assertTrue(fullMonths.contains(java.time.YearMonth.of(2026, 2)));
        assertTrue(fullMonths.contains(java.time.YearMonth.of(2026, 3)));
        assertFalse(fullMonths.contains(java.time.YearMonth.of(2026, 1)));
    }

    @Test
    @DisplayName("16. Large transaction does not distort its own reference median")
    void testLargeTransactionDoesNotDistortOwnMedian() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // 3 transactions: 100, 100, and 1000.
        // If including 1000 in median, median is 100.
        // Peer median excluding 1000 is still 100.
        // 1000 is 10x peer median -> flagged as unusual!
        txs.add(createExpense("t1", LocalDate.of(2026, 1, 5), "Lunch", new BigDecimal("100.00"), "Food & Dining", "Diner"));
        txs.add(createExpense("t2", LocalDate.of(2026, 1, 10), "Lunch", new BigDecimal("100.00"), "Food & Dining", "Diner"));
        txs.add(createExpense("t3", LocalDate.of(2026, 1, 15), "Banquet", new BigDecimal("1000.00"), "Food & Dining", "Diner"));

        List<FinancialInsight> insights = detector.detectUnusuallyLargeExpenses(txs, null, null);
        assertEquals(1, insights.size());
        assertTrue(insights.get(0).getCalculationEvidence().contains("Peer Category Median"));
    }
}

