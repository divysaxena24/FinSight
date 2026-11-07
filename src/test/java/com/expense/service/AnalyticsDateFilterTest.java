package com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.TransactionDAO;
import com.expense.dao.UserDAO;
import com.expense.model.User;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsDateFilterTest {

    private static TransactionService transactionService;
    private static TransactionDAO transactionDAO;
    private static UserDAO userDAO;
    private static int userAId;
    private static int userEmptyId;

    // Fixed clock: 2024-02-29 (Leap Year test date)
    private static final Clock leapYearClock = Clock.fixed(
            Instant.parse("2024-02-29T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeAll
    public static void setUp() {
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
}

