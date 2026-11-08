package com.finsight.engine.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.FinancialStatementReport.MerchantSpend;
import com.finsight.engine.model.FinancialStatementReport.MonthlyCashFlow;
import com.finsight.engine.model.MetricSummary;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AnalyticsCalculator Step 1.2 Final Analytics Contract Tests")
public class AnalyticsCalculatorTest {

    private AnalyticsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AnalyticsCalculator();
    }

    private ParsedTransaction createTx(String id, LocalDate date, String desc,
                                       BigDecimal amount, TransactionType type,
                                       String merchant, String category) {
        return new ParsedTransaction(
                id, date, desc,
                type == TransactionType.EXPENSE ? amount : null,
                type == TransactionType.INCOME ? amount : null,
                amount, type, null, merchant, category,
                new BigDecimal("0.95"), desc, "TEST_FORMAT",
                false, false, false, false
        );
    }

    @Test
    @DisplayName("1. Normal income and expenses calculation with gross and net separation")
    void testNormalIncomeAndExpenses() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Salary Credited", new BigDecimal("50000.00"), TransactionType.INCOME, "Employer", "Salary"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 5), "Supermarket Grocery", new BigDecimal("12000.00"), TransactionType.EXPENSE, "Supermarket", "Food & Dining"));
        txs.add(createTx("3", LocalDate.of(2026, 3, 10), "Electricity Bill", new BigDecimal("3000.00"), TransactionType.EXPENSE, "Power Corp", "Utilities"));

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("50000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("15000.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("0.00"), summary.getTotalRefunds());
        assertEquals(new BigDecimal("15000.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("35000.00"), summary.getNetCashFlow());
        assertEquals(new BigDecimal("70.00"), summary.getSavingsRate());
        assertEquals(new BigDecimal("12000.00"), summary.getHighestExpense());
        assertEquals(new BigDecimal("7500.00"), summary.getAverageGrossExpense()); // 15000 / 2
        assertEquals(new BigDecimal("50000.00"), summary.getAverageIncomeDeposit());
        assertEquals(3, summary.getTotalTransactionCount());
        assertEquals(1, summary.getIncomeTransactionCount());
        assertEquals(2, summary.getExpenseTransactionCount());
    }

    @Test
    @DisplayName("2. Zero income handled safely without division by zero")
    void testZeroIncome() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Coffee", new BigDecimal("250.00"), TransactionType.EXPENSE, "Cafe", "Food & Dining"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 2), "Snacks", new BigDecimal("150.00"), TransactionType.EXPENSE, "Store", "Food & Dining"));

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("0.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("400.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("400.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("-400.00"), summary.getNetCashFlow());
        assertEquals(new BigDecimal("0.00"), summary.getSavingsRate());
        assertEquals(new BigDecimal("0.00"), summary.getAverageIncomeDeposit());
        assertEquals(new BigDecimal("200.00"), summary.getAverageGrossExpense());
    }

    @Test
    @DisplayName("3. Fractional paise/cents with Banker's Rounding")
    void testFractionalPaiseCents() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Fractional Income", new BigDecimal("100.125"), TransactionType.INCOME, "Client", "Salary"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 2), "Fractional Expense", new BigDecimal("50.125"), TransactionType.EXPENSE, "Vendor", "Shopping"));

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("100.12"), summary.getTotalIncome());
        assertEquals(new BigDecimal("50.12"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("50.12"), summary.getNetExpenses());
        assertEquals(new BigDecimal("50.00"), summary.getNetCashFlow());
    }

    @Test
    @DisplayName("4. Empty transaction list returns clean zero state")
    void testEmptyTransactionList() {
        FinancialStatementReport report = calculator.calculateReport(new ArrayList<>());
        MetricSummary summary = report.getSummary();

        assertNotNull(summary);
        assertEquals(new BigDecimal("0.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("0.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("0.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("0.00"), summary.getNetCashFlow());
        assertEquals(0, summary.getTotalTransactionCount());
        assertTrue(report.getCategoryExpenses().isEmpty());
        assertTrue(report.getOutOfPeriodTransactions().isEmpty());
    }

    @Test
    @DisplayName("5. Transfer exclusion from operating totals")
    void testTransferExclusion() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Salary", new BigDecimal("40000.00"), TransactionType.INCOME, "Work", "Salary"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 3), "Rent", new BigDecimal("15000.00"), TransactionType.EXPENSE, "Landlord", "Housing & Rent"));

        ParsedTransaction transferTx1 = createTx("3", LocalDate.of(2026, 3, 4), "Transfer to Savings", new BigDecimal("10000.00"), TransactionType.TRANSFER, "Self", "Transfers");
        txs.add(transferTx1);

        ParsedTransaction transferTx2 = createTx("4", LocalDate.of(2026, 3, 5), "NEFT to Mom", new BigDecimal("5000.00"), TransactionType.EXPENSE, "Mom", "Transfer");
        txs.add(transferTx2);

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("40000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("15000.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("15000.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("25000.00"), summary.getNetCashFlow());
        assertEquals(2, summary.getTransferTransactionCount());
    }

    @Test
    @DisplayName("6. Suspected duplicates are preserved and included; confirmed duplicates are excluded")
    void testSuspectedVsConfirmedDuplicates() {
        List<ParsedTransaction> txs = new ArrayList<>();
        ParsedTransaction tx1 = createTx("1", LocalDate.of(2026, 3, 1), "Coffee", new BigDecimal("250.00"), TransactionType.EXPENSE, "Starbucks", "Food & Dining");
        ParsedTransaction tx2 = createTx("2", LocalDate.of(2026, 3, 1), "Coffee", new BigDecimal("250.00"), TransactionType.EXPENSE, "Starbucks", "Food & Dining");
        tx2.setDuplicateCandidate(true); // Suspected duplicate

        ParsedTransaction tx3 = createTx("3", LocalDate.of(2026, 3, 1), "Coffee", new BigDecimal("250.00"), TransactionType.EXPENSE, "Starbucks", "Food & Dining");
        tx3.setConfirmedDuplicate(true);

        txs.add(tx1);
        txs.add(tx2);
        txs.add(tx3);

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("500.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("500.00"), summary.getNetExpenses());
        assertEquals(1, summary.getSuspectedDuplicateCount());
        assertEquals(new BigDecimal("250.00"), summary.getSuspectedDuplicateAmount());
        assertEquals(1, summary.getConfirmedDuplicateCount());
        assertEquals(new BigDecimal("250.00"), summary.getConfirmedDuplicateAmount());
        assertEquals(3, report.getTransactions().size());
    }

    @Test
    @DisplayName("7. Multi-dimensional refund reconciliation across categories, merchants, daily, and monthly")
    void testRefundMultiDimensionalReconciliation() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Salary", new BigDecimal("80000.00"), TransactionType.INCOME, "TechCorp", "Salary"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 2), "Amazon Electronics Purchase", new BigDecimal("5000.00"), TransactionType.EXPENSE, "Amazon", "Shopping"));
        txs.add(createTx("3", LocalDate.of(2026, 3, 3), "Zomato Dinner", new BigDecimal("1000.00"), TransactionType.EXPENSE, "Zomato", "Food & Dining"));

        ParsedTransaction refundTx = createTx("4", LocalDate.of(2026, 3, 10), "Amazon Return Refund", new BigDecimal("5000.00"), TransactionType.INCOME, "Amazon", "Shopping");
        refundTx.setRefundOrReversal(true);
        txs.add(refundTx);

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("80000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("6000.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalRefunds());
        assertEquals(new BigDecimal("1000.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("79000.00"), summary.getNetCashFlow());

        Map<String, BigDecimal> catNet = report.getCategoryExpenses();
        assertEquals(new BigDecimal("0.00"), catNet.get("Shopping"));
        assertEquals(new BigDecimal("1000.00"), catNet.get("Food & Dining"));

        BigDecimal sumCategoryNet = catNet.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(summary.getNetExpenses(), sumCategoryNet);

        MerchantSpend amazonSpend = report.getTopMerchants().stream()
                .filter(m -> m.getMerchant().equals("Amazon"))
                .findFirst().orElse(null);
        assertNotNull(amazonSpend);
        assertEquals(new BigDecimal("5000.00"), amazonSpend.getGrossSpend());
        assertEquals(new BigDecimal("5000.00"), amazonSpend.getRefundAmount());
        assertEquals(new BigDecimal("0.00"), amazonSpend.getNetSpend());

        MonthlyCashFlow marchFlow = report.getMonthlyCashFlows().get(YearMonth.of(2026, 3));
        assertNotNull(marchFlow);
        assertEquals(new BigDecimal("6000.00"), marchFlow.getGrossExpense());
        assertEquals(new BigDecimal("5000.00"), marchFlow.getRefunds());
        assertEquals(new BigDecimal("1000.00"), marchFlow.getNetExpense());

        Map<LocalDate, BigDecimal> daily = report.getDailyExpenses();
        BigDecimal sumDaily = daily.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(summary.getNetExpenses(), sumDaily);
    }

    @Test
    @DisplayName("8. Refund excess flag (hasRefundExcess) when refunds exceed period gross expenses")
    void testRefundExcessFlaggedWithoutUnsubstantiatedCrossPeriodClaim() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Coffee", new BigDecimal("200.00"), TransactionType.EXPENSE, "Cafe", "Food & Dining"));

        ParsedTransaction largeRefund = createTx("2", LocalDate.of(2026, 3, 5), "Returned Electronics", new BigDecimal("15000.00"), TransactionType.INCOME, "Store", "Shopping");
        largeRefund.setRefundOrReversal(true);
        txs.add(largeRefund);

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("200.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("15000.00"), summary.getTotalRefunds());
        assertEquals(new BigDecimal("-14800.00"), summary.getNetExpenses());
        assertTrue(summary.isHasRefundExcess());
        assertTrue(summary.isHasCrossPeriodRefunds()); // backward-compatible alias
        assertFalse(report.getWarnings().isEmpty());
        assertTrue(report.getWarnings().get(0).contains("resulting in net negative expenditure"));
    }

    @Test
    @DisplayName("9. Out-of-period transactions are completely isolated from metrics but preserved in report")
    void testOutOfPeriodTransactionIsolation() {
        List<ParsedTransaction> txs = new ArrayList<>();
        // Before statement period (Feb 25)
        ParsedTransaction earlyTx = createTx("1", LocalDate.of(2026, 2, 25), "Early Expense", new BigDecimal("1000.00"), TransactionType.EXPENSE, "Store", "Shopping");
        // Inside statement period (March 1 to March 31)
        ParsedTransaction inTx1 = createTx("2", LocalDate.of(2026, 3, 5), "In-Period Salary", new BigDecimal("50000.00"), TransactionType.INCOME, "Work", "Salary");
        ParsedTransaction inTx2 = createTx("3", LocalDate.of(2026, 3, 10), "In-Period Grocery", new BigDecimal("4000.00"), TransactionType.EXPENSE, "Blinkit", "Food & Dining");
        // After statement period (April 2)
        ParsedTransaction lateTx = createTx("4", LocalDate.of(2026, 4, 2), "Late Refund", new BigDecimal("500.00"), TransactionType.INCOME, "Shop", "Shopping");
        lateTx.setRefundOrReversal(true);

        txs.add(earlyTx);
        txs.add(inTx1);
        txs.add(inTx2);
        txs.add(lateTx);

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        FinancialStatementReport report = calculator.calculateReport(txs, start, end);
        MetricSummary summary = report.getSummary();

        // 1. In-period metrics only: earlyTx (1000) and lateTx (500) are excluded
        assertEquals(new BigDecimal("50000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("4000.00"), summary.getGrossExpenses());
        assertEquals(new BigDecimal("0.00"), summary.getTotalRefunds());
        assertEquals(new BigDecimal("4000.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("46000.00"), summary.getNetCashFlow());
        assertEquals(2, summary.getTotalTransactionCount());
        assertEquals(2, summary.getOutOfPeriodTransactionCount());

        // 2. All 4 transactions preserved in getTransactions()
        assertEquals(4, report.getTransactions().size());

        // 3. Exactly 2 out-of-period transactions in getOutOfPeriodTransactions()
        List<ParsedTransaction> outList = report.getOutOfPeriodTransactions();
        assertEquals(2, outList.size());
        assertTrue(outList.contains(earlyTx));
        assertTrue(outList.contains(lateTx));

        // 4. Category reconciliation includes only in-period categories
        assertFalse(report.getCategoryExpenses().containsKey("Shopping"));
        assertEquals(new BigDecimal("4000.00"), report.getCategoryExpenses().get("Food & Dining"));

        // 5. Merchant reconciliation includes only in-period merchants
        assertEquals(1, report.getTopMerchants().size());
        assertEquals("Blinkit", report.getTopMerchants().get(0).getMerchant());

        // 6. Daily average uses 31 declared statement days (4000 / 31 = 129.03)
        assertEquals(new BigDecimal("129.03"), summary.getAverageDailySpend());
    }

    @Test
    @DisplayName("10. Invalid statement period dates throw IllegalArgumentException")
    void testInvalidStatementPeriodDates() {
        List<ParsedTransaction> txs = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 3, 31);
        LocalDate end = LocalDate.of(2026, 3, 1);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateReport(txs, start, end));
    }

    @Test
    @DisplayName("11. Fallback behavior when declared dates are omitted")
    void testFallbackBehaviorWhenDeclaredDatesOmitted() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 5), "Purchase", new BigDecimal("1000.00"), TransactionType.EXPENSE, "Shop", "Shopping"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 10), "Purchase 2", new BigDecimal("2000.00"), TransactionType.EXPENSE, "Shop", "Shopping"));

        FinancialStatementReport report = calculator.calculateReport(txs, null, null);
        MetricSummary summary = report.getSummary();

        // Effective dates fall back to March 5 and March 10 (6 days)
        assertEquals(LocalDate.of(2026, 3, 5), report.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 3, 10), report.getEffectiveEndDate());
        // 3000 / 6 days = 500.00 / day
        assertEquals(new BigDecimal("500.00"), summary.getAverageDailySpend());
        assertTrue(report.getOutOfPeriodTransactions().isEmpty());
    }

    @Test
    @DisplayName("12. Negative cash flow when expenses exceed income")
    void testNegativeCashFlow() {
        List<ParsedTransaction> txs = new ArrayList<>();
        txs.add(createTx("1", LocalDate.of(2026, 3, 1), "Part-time Pay", new BigDecimal("10000.00"), TransactionType.INCOME, "Freelance", "Salary"));
        txs.add(createTx("2", LocalDate.of(2026, 3, 2), "Car Repair", new BigDecimal("18000.00"), TransactionType.EXPENSE, "Auto Workshop", "Transport"));

        FinancialStatementReport report = calculator.calculateReport(txs);
        MetricSummary summary = report.getSummary();

        assertEquals(new BigDecimal("10000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("18000.00"), summary.getNetExpenses());
        assertEquals(new BigDecimal("-8000.00"), summary.getNetCashFlow());
        assertEquals(new BigDecimal("-80.00"), summary.getSavingsRate());
    }
}
