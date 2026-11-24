package com.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.TransactionDAO;
import com.expense.dao.UserDAO;
import com.expense.dao.PasswordResetDAO;
import com.expense.model.Transaction;
import com.expense.model.User;
import com.expense.util.DBConnection;
import com.finsight.simulator.dao.IntelligenceDAO;
import com.finsight.simulator.service.BudgetLimitService;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SQLiteMigrationTest {

    private static UserDAO userDAO;
    private static TransactionDAO transactionDAO;
    private static File testDbFile;

    @BeforeAll
    public static void setUp() {
        // Guarantee isolation: point thread-local DB path to a dedicated test database in target/test-data
        File testDir = new File("target/test-data");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        testDbFile = new File(testDir, "sqlite-migration-test-" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbFile.getAbsolutePath());

        userDAO = new UserDAO();
        transactionDAO = new TransactionDAO();

        // Initialize schema and ensure clean test state on the dedicated test database
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con != null ? con.createStatement() : null) {
            if (stmt != null) {
                stmt.execute("DELETE FROM transactions;");
                stmt.execute("DELETE FROM user;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void tearDown() {
        // Clear thread-local DB path
        DBConnection.clearThreadDatabasePath();
        if (testDbFile != null && testDbFile.exists()) {
            testDbFile.delete();
        }
    }

    @Test
    @Order(1)
    public void testDatabaseConnectionAndForeignKeyEnforcement() throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            assertNotNull(con, "Connection to SQLite database should not be null");

            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                assertTrue(rs.next(), "PRAGMA foreign_keys result expected");
                assertEquals(1, rs.getInt(1), "SQLite foreign_keys must be enabled (1)");
            }
        }
    }

    @Test
    @Order(2)
    public void testUserRegistrationAndDuplicateHandling() {
        User user1 = new User();
        user1.setName("Alice Sharma");
        user1.setEmail("alice@example.com");
        user1.setPassword("SecurePass123!");

        boolean registered = userDAO.registerUser(user1);
        assertTrue(registered, "User registration should succeed");

        // Attempt duplicate registration with same email
        User duplicateUser = new User();
        duplicateUser.setName("Alice Duplicate");
        duplicateUser.setEmail("alice@example.com");
        duplicateUser.setPassword("AnotherPass!");

        boolean dupRegistered = userDAO.registerUser(duplicateUser);
        assertFalse(dupRegistered, "Duplicate registration with existing email must fail");

        User caseVariantDuplicate = new User();
        caseVariantDuplicate.setName("Alice Case Duplicate");
        caseVariantDuplicate.setEmail("ALICE@EXAMPLE.COM");
        caseVariantDuplicate.setPassword("AnotherPass!");

        boolean caseVariantRegistered = userDAO.registerUser(caseVariantDuplicate);
        assertFalse(caseVariantRegistered, "Email duplicates must be case-insensitive");
    }

    @Test
    @Order(3)
    public void testUserLoginWithBCryptVerification() {
        // Valid credentials
        User loggedIn = userDAO.loginUser("alice@example.com", "SecurePass123!");
        assertNotNull(loggedIn, "User login with correct credentials should return User");
        assertEquals("Alice Sharma", loggedIn.getName());
        assertTrue(loggedIn.getId() > 0, "User ID should be generated");

        // Invalid password
        User wrongPass = userDAO.loginUser("alice@example.com", "WrongPassword");
        assertNull(wrongPass, "Login with invalid password must return null");

        // Non-existent email
        User notFound = userDAO.loginUser("ghost@example.com", "SecurePass123!");
        assertNull(notFound, "Login with non-existent email must return null");
    }

    @Test
    @Order(4)
    public void testForeignKeyConstraintEnforcement() {
        // Attempt to insert transaction for non-existent user_id 99999
        Transaction orphanTransaction = new Transaction();
        orphanTransaction.setUserId(99999);
        orphanTransaction.setType("Expense");
        orphanTransaction.setCategory("Food");
        orphanTransaction.setAmount(150.0);
        orphanTransaction.setDescription("Orphan transaction");
        orphanTransaction.setTransactionDate(new Date(System.currentTimeMillis()));

        boolean added = transactionDAO.addTransaction(orphanTransaction);
        assertFalse(added, "Inserting transaction for non-existent user must fail due to foreign key constraint");
    }

    @Test
    @Order(5)
    public void testTransactionCRUDAndPrecision() {
        User alice = userDAO.loginUser("alice@example.com", "SecurePass123!");
        assertNotNull(alice);

        // 1. Add Income
        Transaction income = new Transaction();
        income.setUserId(alice.getId());
        income.setType("Income");
        income.setCategory("Salary");
        income.setAmount(50000.50);
        income.setDescription("Monthly Salary");
        income.setTransactionDate(Date.valueOf("2026-11-01"));

        boolean incomeAdded = transactionDAO.addTransaction(income);
        assertTrue(incomeAdded, "Income transaction should be added successfully");

        // 2. Add Expense
        Transaction expense = new Transaction();
        expense.setUserId(alice.getId());
        expense.setType("Expense");
        expense.setCategory("Rent");
        expense.setAmount(15000.25);
        expense.setDescription("Apartment Rent");
        expense.setTransactionDate(Date.valueOf("2026-11-02"));

        boolean expenseAdded = transactionDAO.addTransaction(expense);
        assertTrue(expenseAdded, "Expense transaction should be added successfully");

        // 3. Verify totals
        double totalIncome = transactionDAO.getTotalIncome(alice.getId());
        assertEquals(50000.50, totalIncome, 0.001, "Total income precision check");

        double totalExpense = transactionDAO.getTotalExpense(alice.getId());
        assertEquals(15000.25, totalExpense, 0.001, "Total expense precision check");

        // 4. List transactions
        List<Transaction> list = transactionDAO.getTransactionsByUser(alice.getId());
        assertEquals(2, list.size(), "Alice should have 2 transactions");

        // 5. Update transaction
        Transaction firstTx = list.get(0);
        firstTx.setAmount(16000.75);
        firstTx.setDescription("Updated Rent with Utilities");
        boolean updated = transactionDAO.updateTransaction(firstTx, alice.getId());
        assertTrue(updated, "Transaction update with correct ownership should succeed");

        Transaction updatedTx = transactionDAO.getTransactionById(firstTx.getId(), alice.getId());
        assertEquals(16000.75, updatedTx.getAmount(), 0.001);
        assertEquals("Updated Rent with Utilities", updatedTx.getDescription());
    }

    @Test
    @Order(6)
    public void testTransactionUserOwnershipIsolation() {
        // Register second user Bob
        User bob = new User();
        bob.setName("Bob Patel");
        bob.setEmail("bob@example.com");
        bob.setPassword("BobPassword123!");
        assertTrue(userDAO.registerUser(bob));

        User loggedInBob = userDAO.loginUser("bob@example.com", "BobPassword123!");
        assertNotNull(loggedInBob);

        User alice = userDAO.loginUser("alice@example.com", "SecurePass123!");
        List<Transaction> aliceList = transactionDAO.getTransactionsByUser(alice.getId());
        assertFalse(aliceList.isEmpty());
        int aliceTxId = aliceList.get(0).getId();

        // Bob attempts to fetch Alice's transaction by ID
        Transaction unauthorizedGet = transactionDAO.getTransactionById(aliceTxId, loggedInBob.getId());
        assertNull(unauthorizedGet, "Bob must NOT be able to access Alice's transaction");

        // Bob attempts to update Alice's transaction
        Transaction forgedTx = new Transaction();
        forgedTx.setId(aliceTxId);
        forgedTx.setType("Income");
        forgedTx.setCategory("Hack");
        forgedTx.setAmount(999999.0);
        forgedTx.setDescription("Forged update");
        forgedTx.setTransactionDate(new Date(System.currentTimeMillis()));

        boolean forgedUpdate = transactionDAO.updateTransaction(forgedTx, loggedInBob.getId());
        assertFalse(forgedUpdate, "Bob must NOT be able to update Alice's transaction");

        // Bob attempts to delete Alice's transaction
        boolean forgedDelete = transactionDAO.deleteTransaction(aliceTxId, loggedInBob.getId());
        assertFalse(forgedDelete, "Bob must NOT be able to delete Alice's transaction");

        // Alice can still delete her own transaction
        boolean legitDelete = transactionDAO.deleteTransaction(aliceTxId, alice.getId());
        assertTrue(legitDelete, "Alice should be able to delete her own transaction");
    }

    @Test
    @Order(7)
    public void testPasswordResetTokenLifecycle() {
        User alice = userDAO.loginUser("alice@example.com", "SecurePass123!");
        assertNotNull(alice);

        PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
        String token = passwordResetDAO.createToken(alice.getId());
        assertNotNull(token);
        assertTrue(passwordResetDAO.isTokenValid(token));

        PasswordResetDAO.ResetResult result =
                passwordResetDAO.resetPassword(token, "ResetPass123!");
        assertEquals(PasswordResetDAO.ResetResult.SUCCESS, result);
        assertFalse(passwordResetDAO.isTokenValid(token));

        assertNotNull(userDAO.loginUser("alice@example.com", "ResetPass123!"));
        assertNull(userDAO.loginUser("alice@example.com", "SecurePass123!"));
    }

    @Test
    @Order(8)
    public void testCategoryBudgetLimitAndHealthcareExemption() {
        User alice = userDAO.loginUser("alice@example.com", "ResetPass123!");
        assertNotNull(alice);

        IntelligenceDAO intelligenceDAO = new IntelligenceDAO();
        String monthYear = "2026-11";
        assertTrue(intelligenceDAO.saveOrUpdateBudget(
                alice.getId(), "Shopping", new BigDecimal("100.00"), monthYear));

        BudgetLimitService limitService = new BudgetLimitService(intelligenceDAO);
        LocalDate date = LocalDate.of(2026, 11, 20);

        BudgetLimitService.CheckResult blocked =
                limitService.checkExpense(alice.getId(), "Shopping", date, new BigDecimal("120.00"));
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getMessage().contains("budget limit exceeded"));

        BudgetLimitService.CheckResult healthcare =
                limitService.checkExpense(alice.getId(), "Healthcare", date, new BigDecimal("9999.00"));
        assertTrue(healthcare.isAllowed());
    }
}

