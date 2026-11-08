package com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.TransactionDAO;
import com.expense.dao.UserDAO;
import com.expense.model.Transaction;
import com.expense.model.User;
import com.expense.util.DBConnection;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsDateFilterTest {

    private static TransactionService transactionService;
    private static TransactionDAO transactionDAO;
    private static UserDAO userDAO;
    private static int userAId;
    private static int userEmptyId;
    private static File testDbFile;

    // Fixed clock: 2024-02-29 (Leap Year test date)
    private static final Clock leapYearClock = Clock.fixed(
            Instant.parse("2024-02-29T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeAll
    public static void setUp() {
        // Guarantee isolation: point thread-local DB path to dedicated test database in target/test-data
        File testDir = new File("target/test-data");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        testDbFile = new File(testDir, "analytics-test-" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbFile.getAbsolutePath());

        transactionDAO = new TransactionDAO();
        userDAO = new UserDAO();
        transactionService = new TransactionService(transactionDAO, leapYearClock);

        // Create User A with transactions on specific dates
        User uA = new User();
        uA.setName("Analytics User A");
        uA.setEmail("analytics_a@example.com");
        uA.setPassword("Secret123!");
        userDAO.registerUser(uA);

        User loggedInA = userDAO.loginUser("analytics_a@example.com", "Secret123!");
        assertNotNull(loggedInA);
        userAId = loggedInA.getId();

        // Create Empty User (no transactions)
        User uEmpty = new User();
        uEmpty.setName("Empty User");
        uEmpty.setEmail("empty_user@example.com");
        uEmpty.setPassword("Secret123!");
        userDAO.registerUser(uEmpty);

        User loggedInEmpty = userDAO.loginUser("empty_user@example.com", "Secret123!");
        assertNotNull(loggedInEmpty);
        userEmptyId = loggedInEmpty.getId();

        // Insert controlled transactions for User A:
        // 1. Within 1M (2024-02-15): Income 10000.00
        transactionService.createTransaction(userAId, "Income", "Salary", 10000.00, "Feb Salary", "2024-02-15");
        // 2. Within 1M (2024-02-20): Expense 2500.00
        transactionService.createTransaction(userAId, "Expense", "Groceries", 2500.00, "Food", "2024-02-20");
        // 3. Within 6M but outside 1M (2023-11-15): Expense 5000.00
        transactionService.createTransaction(userAId, "Expense", "Rent", 5000.00, "Nov Rent", "2023-11-15");
        // 4. Within 1Y but outside 6M (2023-05-10): Income 20000.00
        transactionService.createTransaction(userAId, "Income", "Bonus", 20000.00, "May Bonus", "2023-05-10");
        // 5. Older than 1Y (2022-01-01): Income 50000.00
        transactionService.createTransaction(userAId, "Income", "Old Income", 50000.00, "Historic", "2022-01-01");
    }

    @Test
    @Order(1)
    public void testDateFilter1MAndLeapYear() {
        // Today is 2024-02-29. 1M range: 2024-01-29 to 2024-02-29
        TransactionService.DateRangeResolution range = transactionService.resolveDateRange(userAId, "1M", null, null);
        assertEquals("2024-01-29", range.getStartDateString());
        assertEquals("2024-02-29", range.getEndDateString());
        assertFalse(range.hasError());

        TransactionService.DashboardSummary summary = transactionService.getFilteredDashboardSummary(userAId, "1M", null, null);
        assertEquals(10000.00, summary.getTotalIncome(), 0.001);
        assertEquals(2500.00, summary.getTotalExpense(), 0.001);
        assertEquals(7500.00, summary.getBalance(), 0.001);
        assertEquals(2, summary.getTransactionCount());
    }

    @Test
    @Order(2)
    public void testDateFilter6MAnd1Y() {
        // 6M: 2023-08-29 to 2024-02-29
        TransactionService.DateRangeResolution r6m = transactionService.resolveDateRange(userAId, "6M", null, null);
        assertEquals("2023-08-29", r6m.getStartDateString());
        assertEquals("2024-02-29", r6m.getEndDateString());

        TransactionService.DashboardSummary s6m = transactionService.getFilteredDashboardSummary(userAId, "6M", null, null);
        // Includes 2024-02-15 (+10000), 2024-02-20 (-2500), 2023-11-15 (-5000)
        assertEquals(10000.00, s6m.getTotalIncome(), 0.001);
        assertEquals(7500.00, s6m.getTotalExpense(), 0.001);
        assertEquals(2500.00, s6m.getBalance(), 0.001);
        assertEquals(3, s6m.getTransactionCount());

        // 1Y: 2023-02-29 does not exist in 2023, LocalDate.minusYears(1) on 2024-02-29 safely yields 2023-02-28
        TransactionService.DateRangeResolution r1y = transactionService.resolveDateRange(userAId, "1Y", null, null);
        assertEquals("2023-02-28", r1y.getStartDateString());
        assertEquals("2024-02-29", r1y.getEndDateString());

        TransactionService.DashboardSummary s1y = transactionService.getFilteredDashboardSummary(userAId, "1Y", null, null);
        // Also includes 2023-05-10 (+20000)
        assertEquals(30000.00, s1y.getTotalIncome(), 0.001);
        assertEquals(7500.00, s1y.getTotalExpense(), 0.001);
        assertEquals(22500.00, s1y.getBalance(), 0.001);
        assertEquals(4, s1y.getTransactionCount());
    }

    @Test
    @Order(3)
    public void testDateFilterAllTime() {
        // Earliest for User A is 2022-01-01
        TransactionService.DateRangeResolution rAll = transactionService.resolveDateRange(userAId, "ALL", null, null);
        assertEquals("2022-01-01", rAll.getStartDateString());
        assertEquals("2024-02-29", rAll.getEndDateString());

        TransactionService.DashboardSummary sAll = transactionService.getFilteredDashboardSummary(userAId, "ALL", null, null);
        // All 5 transactions: Income 10000+20000+50000=80000, Expense 2500+5000=7500
        assertEquals(80000.00, sAll.getTotalIncome(), 0.001);
        assertEquals(7500.00, sAll.getTotalExpense(), 0.001);
        assertEquals(72500.00, sAll.getBalance(), 0.001);
        assertEquals(5, sAll.getTransactionCount());
    }

    @Test
    @Order(4)
    public void testAllTimeForEmptyUser() {
        // User with no transactions should return today through today with 0 values
        TransactionService.DateRangeResolution rEmpty = transactionService.resolveDateRange(userEmptyId, "ALL", null, null);
        assertEquals("2024-02-29", rEmpty.getStartDateString());
        assertEquals("2024-02-29", rEmpty.getEndDateString());

        TransactionService.DashboardSummary sEmpty = transactionService.getFilteredDashboardSummary(userEmptyId, "ALL", null, null);
        assertEquals(0.00, sEmpty.getTotalIncome(), 0.001);
        assertEquals(0.00, sEmpty.getTotalExpense(), 0.001);
        assertEquals(0.00, sEmpty.getBalance(), 0.001);
        assertEquals(0, sEmpty.getTransactionCount());
    }

    @Test
    @Order(5)
    public void testCustomDateRangeValid() {
        // Custom: 2023-11-01 to 2023-11-30
        TransactionService.DashboardSummary sCustom = transactionService.getFilteredDashboardSummary(
                userAId, "CUSTOM", "2023-11-01", "2023-11-30");

        assertFalse(sCustom.getDateRange().hasError());
        assertEquals(0.00, sCustom.getTotalIncome(), 0.001);
        assertEquals(5000.00, sCustom.getTotalExpense(), 0.001);
        assertEquals(-5000.00, sCustom.getBalance(), 0.001);
        assertEquals(1, sCustom.getTransactionCount());
    }

    @Test
    @Order(6)
    public void testCustomDateValidationErrors() {
        // 1. Missing dates
        TransactionService.DateRangeResolution r1 = transactionService.resolveDateRange(userAId, "CUSTOM", "", "2024-02-29");
        assertTrue(r1.hasError());
        assertEquals("Both start date and end date are required for custom range.", r1.getErrorMessage());

        // 2. Malformed start date
        TransactionService.DateRangeResolution r2 = transactionService.resolveDateRange(userAId, "CUSTOM", "01-02-2024", "2024-02-29");
        assertTrue(r2.hasError());
        assertEquals("Invalid start date format. Please use YYYY-MM-DD.", r2.getErrorMessage());

        // 3. Start date after end date
        TransactionService.DateRangeResolution r3 = transactionService.resolveDateRange(userAId, "CUSTOM", "2024-03-01", "2024-02-01");
        assertTrue(r3.hasError());
        assertEquals("Start date cannot be after end date.", r3.getErrorMessage());
    }

    @Test
    @Order(7)
    public void testEmptyTransactionRangeAndUserIsolation() {
        // Range where User A has no records (e.g. 2025-01-01 to 2025-01-31)
        TransactionService.DashboardSummary sFuture = transactionService.getFilteredDashboardSummary(
                userAId, "CUSTOM", "2025-01-01", "2025-01-31");
        assertEquals(0.00, sFuture.getTotalIncome(), 0.001);
        assertEquals(0.00, sFuture.getTotalExpense(), 0.001);
        assertEquals(0.00, sFuture.getBalance(), 0.001);
        assertEquals(0, sFuture.getTransactionCount());

        // Empty user querying same active range as User A gets 0 (user isolation check)
        TransactionService.DashboardSummary sEmptyIsolated = transactionService.getFilteredDashboardSummary(
                userEmptyId, "1M", null, null);
        assertEquals(0.00, sEmptyIsolated.getTotalIncome(), 0.001);
        assertEquals(0.00, sEmptyIsolated.getTotalExpense(), 0.001);
        assertEquals(0, sEmptyIsolated.getTransactionCount());
    }

    @Test
    @Order(8)
    public void testMonthlyTrendChartAggregationAndZeroGapFilling() {
        // Test 6M range for User A: 2023-08-29 to 2024-02-29
        // Intersecting calendar months: Aug 2023, Sep 2023, Oct 2023, Nov 2023, Dec 2023, Jan 2024, Feb 2024 (7 months)
        TransactionService.DateRangeResolution r6m = transactionService.resolveDateRange(userAId, "6M", null, null);
        TransactionService.MonthlyTrendChartData chartData = transactionService.getMonthlyTrendChartData(userAId, r6m);

        assertTrue(chartData.isHasData());
        assertEquals(7, chartData.getLabels().size());
        assertEquals("Aug 2023", chartData.getLabels().get(0));
        assertEquals("Nov 2023", chartData.getLabels().get(3));
        assertEquals("Feb 2024", chartData.getLabels().get(6));

        // Check values:
        // Aug 2023: no records -> 0, 0
        assertEquals(0.0, chartData.getIncome().get(0), 0.001);
        assertEquals(0.0, chartData.getExpense().get(0), 0.001);

        // Sep 2023: no records -> 0, 0 (zero gap-fill)
        assertEquals(0.0, chartData.getIncome().get(1), 0.001);
        assertEquals(0.0, chartData.getExpense().get(1), 0.001);

        // Oct 2023: no records -> 0, 0 (zero gap-fill)
        assertEquals(0.0, chartData.getIncome().get(2), 0.001);
        assertEquals(0.0, chartData.getExpense().get(2), 0.001);

        // Nov 2023: Expense 5000.00
        assertEquals(0.0, chartData.getIncome().get(3), 0.001);
        assertEquals(5000.00, chartData.getExpense().get(3), 0.001);

        // Dec 2023: no records -> 0, 0 (zero gap-fill)
        assertEquals(0.0, chartData.getIncome().get(4), 0.001);
        assertEquals(0.0, chartData.getExpense().get(4), 0.001);

        // Jan 2024: no records -> 0, 0 (zero gap-fill)
        assertEquals(0.0, chartData.getIncome().get(5), 0.001);
        assertEquals(0.0, chartData.getExpense().get(5), 0.001);

        // Feb 2024: Income 10000.00, Expense 2500.00
        assertEquals(10000.00, chartData.getIncome().get(6), 0.001);
        assertEquals(2500.00, chartData.getExpense().get(6), 0.001);
    }

    @Test
    @Order(9)
    public void testMonthlyTrendCustomMultiMonth() {
        // Custom 3-month span: 2023-10-15 to 2023-12-15
        TransactionService.DateRangeResolution customRange = transactionService.resolveDateRange(
                userAId, "CUSTOM", "2023-10-15", "2023-12-15");
        TransactionService.MonthlyTrendChartData chartData = transactionService.getMonthlyTrendChartData(userAId, customRange);

        assertTrue(chartData.isHasData());
        assertEquals(3, chartData.getLabels().size());
        assertEquals("Oct 2023", chartData.getLabels().get(0));
        assertEquals("Nov 2023", chartData.getLabels().get(1));
        assertEquals("Dec 2023", chartData.getLabels().get(2));

        // Oct: 0, Nov: 5000, Dec: 0
        assertEquals(0.0, chartData.getExpense().get(0), 0.001);
        assertEquals(5000.0, chartData.getExpense().get(1), 0.001);
        assertEquals(0.0, chartData.getExpense().get(2), 0.001);
    }

    @Test
    @Order(10)
    public void testMonthlyTrendForEmptyUserAndIsolation() {
        // Empty user querying 1Y
        TransactionService.DateRangeResolution r1y = transactionService.resolveDateRange(userEmptyId, "1Y", null, null);
        TransactionService.MonthlyTrendChartData emptyChart = transactionService.getMonthlyTrendChartData(userEmptyId, r1y);

        assertFalse(emptyChart.isHasData(), "Empty user should have hasData = false");
        for (double inc : emptyChart.getIncome()) {
            assertEquals(0.0, inc, 0.001);
        }
        for (double exp : emptyChart.getExpense()) {
            assertEquals(0.0, exp, 0.001);
        }

        // Empty user querying ALL
        TransactionService.DateRangeResolution rAll = transactionService.resolveDateRange(userEmptyId, "ALL", null, null);
        TransactionService.MonthlyTrendChartData emptyAllChart = transactionService.getMonthlyTrendChartData(userEmptyId, rAll);
        assertFalse(emptyAllChart.isHasData());
        assertEquals(1, emptyAllChart.getLabels().size());
        assertEquals(0.0, emptyAllChart.getIncome().get(0), 0.001);
        assertEquals(0.0, emptyAllChart.getExpense().get(0), 0.001);
    }

    @Test
    @Order(11)
    public void testCategoryExpenseBreakdownAggregationAndOrdering() {
        // Range 6M for User A (2023-08-29 to 2024-02-29):
        // Has User A expenses:
        // - 2023-11-15: Rent 5000.00
        // - 2024-02-20: Groceries 2500.00
        // (Income Salary 10000.00 on 2024-02-15 must be EXCLUDED)
        TransactionService.DateRangeResolution r6m = transactionService.resolveDateRange(userAId, "6M", null, null);
        TransactionService.CategoryBreakdownData breakdown = transactionService.getCategoryBreakdownData(userAId, r6m);

        assertTrue(breakdown.isHasData());
        assertEquals(7500.00, breakdown.getTotalExpenses(), 0.001);
        assertEquals(2, breakdown.getItems().size());

        // Order descending by amount: Rent (5000.00) first, Groceries (2500.00) second
        TransactionService.CategoryBreakdownItem item1 = breakdown.getItems().get(0);
        assertEquals("Rent", item1.getCategory());
        assertEquals(5000.00, item1.getTotalExpense(), 0.001);
        assertEquals(1, item1.getTransactionCount());
        // Percentage: 5000 / 7500 = 66.666...% -> 66.7%
        assertEquals(66.7, item1.getPercentage(), 0.1);

        TransactionService.CategoryBreakdownItem item2 = breakdown.getItems().get(1);
        assertEquals("Groceries", item2.getCategory());
        assertEquals(2500.00, item2.getTotalExpense(), 0.001);
        assertEquals(1, item2.getTransactionCount());
        // Percentage: 2500 / 7500 = 33.333...% -> 33.3%
        assertEquals(33.3, item2.getPercentage(), 0.1);

        assertEquals("Rent", breakdown.getLabels().get(0));
        assertEquals("Groceries", breakdown.getLabels().get(1));
    }

    @Test
    @Order(12)
    public void testCategoryExpenseNullAndWhitespaceNormalization() {
        // Create transactions with NULL, empty, and whitespace categories via DAO directly
        // to verify database query and service layer normalization to "Uncategorized"
        Transaction txNull = new Transaction();
        txNull.setUserId(userAId);
        txNull.setType("Expense");
        txNull.setCategory(null);
        txNull.setAmount(1200.00);
        txNull.setDescription("Null category expense");
        txNull.setTransactionDate(java.sql.Date.valueOf("2023-12-05"));
        transactionDAO.addTransaction(txNull);

        Transaction txSpaces = new Transaction();
        txSpaces.setUserId(userAId);
        txSpaces.setType("Expense");
        txSpaces.setCategory("   ");
        txSpaces.setAmount(800.00);
        txSpaces.setDescription("Spaces category expense");
        txSpaces.setTransactionDate(java.sql.Date.valueOf("2023-12-10"));
        transactionDAO.addTransaction(txSpaces);

        // Query custom date window covering December 2023
        TransactionService.DateRangeResolution customDec = transactionService.resolveDateRange(
                userAId, "CUSTOM", "2023-12-01", "2023-12-31");
        TransactionService.CategoryBreakdownData decBreakdown = transactionService.getCategoryBreakdownData(
                userAId, customDec);

        assertTrue(decBreakdown.isHasData());
        assertEquals(2000.00, decBreakdown.getTotalExpenses(), 0.001);
        assertEquals(1, decBreakdown.getItems().size());

        TransactionService.CategoryBreakdownItem uncatItem = decBreakdown.getItems().get(0);
        assertEquals("Uncategorized", uncatItem.getCategory());
        assertEquals(2000.00, uncatItem.getTotalExpense(), 0.001);
        assertEquals(2, uncatItem.getTransactionCount());
        assertEquals(100.0, uncatItem.getPercentage(), 0.01);
    }

    @Test
    @Order(13)
    public void testCategoryExpenseZeroExpensesAndUserIsolation() {
        // Empty user querying 1Y should have no category expense data
        TransactionService.DateRangeResolution empty1y = transactionService.resolveDateRange(
                userEmptyId, "1Y", null, null);
        TransactionService.CategoryBreakdownData emptyBreakdown = transactionService.getCategoryBreakdownData(
                userEmptyId, empty1y);

        assertFalse(emptyBreakdown.isHasData());
        assertEquals(0.00, emptyBreakdown.getTotalExpenses(), 0.001);
        assertTrue(emptyBreakdown.getItems().isEmpty());
        assertTrue(emptyBreakdown.getLabels().isEmpty());
        assertTrue(emptyBreakdown.getData().isEmpty());

        // User A querying a date range with only income (e.g. 2023-05-01 to 2023-05-31 where only Bonus occurred)
        TransactionService.DateRangeResolution incomeOnlyRange = transactionService.resolveDateRange(
                userAId, "CUSTOM", "2023-05-01", "2023-05-31");
        TransactionService.CategoryBreakdownData incomeOnlyBreakdown = transactionService.getCategoryBreakdownData(
                userAId, incomeOnlyRange);

        assertFalse(incomeOnlyBreakdown.isHasData());
        assertEquals(0.00, incomeOnlyBreakdown.getTotalExpenses(), 0.001);
        assertTrue(incomeOnlyBreakdown.getItems().isEmpty());
    }

    @AfterAll
    public static void tearDown() {
        DBConnection.clearThreadDatabasePath();
        if (testDbFile != null && testDbFile.exists()) {
            testDbFile.delete();
        }
    }
}

