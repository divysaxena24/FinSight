package com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.UserDAO;
import com.expense.model.Transaction;
import com.expense.model.User;
import com.expense.util.DBConnection;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionServiceTest {

    private static TransactionService transactionService;
    private static UserDAO userDAO;
    private static int testUserId;
    private static int otherUserId;
    private static File testDbFile;

    @BeforeAll
    public static void setup() {
        // Guarantee isolation: point thread-local DB path to dedicated test database in target/test-data
        File testDir = new File("target/test-data");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        testDbFile = new File(testDir, "transaction-service-test-" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbFile.getAbsolutePath());

        transactionService = new TransactionService();
        userDAO = new UserDAO();

        // Register distinct service test users
        User u1 = new User();
        u1.setName("Service Tester");
        u1.setEmail("service_tester@example.com");
        u1.setPassword("Secure12345");
        userDAO.registerUser(u1);

        User loggedIn1 = userDAO.loginUser("service_tester@example.com", "Secure12345");
        assertNotNull(loggedIn1);
        testUserId = loggedIn1.getId();

        User u2 = new User();
        u2.setName("Other User");
        u2.setEmail("other_user@example.com");
        u2.setPassword("Secure12345");
        userDAO.registerUser(u2);

        User loggedIn2 = userDAO.loginUser("other_user@example.com", "Secure12345");
        assertNotNull(loggedIn2);
        otherUserId = loggedIn2.getId();
    }

    @Test
    @Order(1)
    public void testCreateTransactionValidation() {
        // Invalid amount
        boolean invalidAmount = transactionService.createTransaction(testUserId, "Income", "Salary", -100.0, "Invalid", "2026-11-01");
        assertFalse(invalidAmount, "Negative amount should be rejected");

        // Invalid zero amount
        boolean zeroAmount = transactionService.createTransaction(testUserId, "Expense", "Food", 0.0, "Invalid", "2026-11-01");
        assertFalse(zeroAmount, "Zero amount should be rejected");

        // Invalid type
        boolean invalidType = transactionService.createTransaction(testUserId, "UnknownType", "Food", 50.0, "Invalid", "2026-11-01");
        assertFalse(invalidType, "Invalid type should be rejected");

        // Valid Income
        boolean validIncome = transactionService.createTransaction(testUserId, "Income", "Salary", 4000.0, "Tech Salary", "2026-11-01");
        assertTrue(validIncome, "Valid income creation should succeed");

        // Valid Expense
        boolean validExpense = transactionService.createTransaction(testUserId, "Expense", "Groceries", 150.50, "Weekly food", "2026-11-02");
        assertTrue(validExpense, "Valid expense creation should succeed");
    }

    @Test
    @Order(2)
    public void testGetDashboardSummary() {
        TransactionService.DashboardSummary summary = transactionService.getDashboardSummary(testUserId);
        assertNotNull(summary);
        assertTrue(summary.getTotalIncome() >= 4000.0);
        assertTrue(summary.getTotalExpense() >= 150.50);
        assertEquals(summary.getTotalIncome() - summary.getTotalExpense(), summary.getBalance(), 0.001);
        assertTrue(summary.getTransactionCount() >= 2);
    }

    @Test
    @Order(3)
    public void testOwnershipEnforcementOnUpdateAndDelete() {
        List<Transaction> userTxs = transactionService.getUserTransactions(testUserId);
        assertFalse(userTxs.isEmpty());
        int targetTxId = userTxs.get(0).getId();

        // Other user attempts to get transaction
        Transaction stolenGet = transactionService.getTransaction(targetTxId, otherUserId);
        assertNull(stolenGet, "Other user must not be able to retrieve transaction");

        // Other user attempts to update
        boolean unauthorizedUpdate = transactionService.updateTransaction(targetTxId, otherUserId, "Income", "Hacked", 9999.0, "Hacked", "2026-11-01");
        assertFalse(unauthorizedUpdate, "Other user must not be able to update transaction");

        // Other user attempts to delete
        boolean unauthorizedDelete = transactionService.deleteTransaction(targetTxId, otherUserId);
        assertFalse(unauthorizedDelete, "Other user must not be able to delete transaction");

        // Legitimate owner deletes
        boolean legitDelete = transactionService.deleteTransaction(targetTxId, testUserId);
        assertTrue(legitDelete, "Owner must be able to delete their transaction");
    }

    @AfterAll
    public static void tearDown() {
        DBConnection.clearThreadDatabasePath();
        if (testDbFile != null && testDbFile.exists()) {
            testDbFile.delete();
        }
    }
}

