package com.finsight.simulator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.UserDAO;
import com.expense.dao.UserDAO.PinVerificationResult;
import com.expense.model.User;
import com.expense.util.DBConnection;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.dao.LedgerDAO.TransactionExecutionResult;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.Account.AccountType;
import com.finsight.simulator.model.LedgerEntry;
import com.finsight.simulator.model.LedgerEntry.EntryType;
import com.finsight.simulator.model.SimulatorTransaction;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FinancialIntegrityAndP0VerificationTest {

    private static Path testDbPath;
    private static UserDAO userDAO;
    private static AccountDAO accountDAO;
    private static LedgerDAO ledgerDAO;

    private static int userAId;
    private static int userBId;
    private static Account accountA;
    private static Account accountB;

    @BeforeAll
    public static void setUp() throws Exception {
        Path testDir = Paths.get("target", "test-data");
        Files.createDirectories(testDir);
        testDbPath = testDir.resolve("p0_integrity_test_" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbPath.toString());

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Test database connection must be valid");
        }

        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
        ledgerDAO = new LedgerDAO();

        // Create User A
        User userA = new User();
        userA.setName("Integrity User A");
        userA.setEmail("usera@finsight.test");
        userA.setPassword("SecretA123");
        userA.setPhone("9876543210");
        userA.setTransactionPin("4321");
        assertTrue(userDAO.registerUser(userA));
        User loggedA = userDAO.loginUser("usera@finsight.test", "SecretA123");
        assertNotNull(loggedA);
        userAId = loggedA.getId();

        // Create User B
        User userB = new User();
        userB.setName("Integrity User B");
        userB.setEmail("userb@finsight.test");
        userB.setPassword("SecretB123");
        userB.setPhone("9876543211");
        userB.setTransactionPin("5678");
        assertTrue(userDAO.registerUser(userB));
        User loggedB = userDAO.loginUser("userb@finsight.test", "SecretB123");
        assertNotNull(loggedB);
        userBId = loggedB.getId();

        // Create Accounts
        accountA = accountDAO.createAccount(userAId, AccountType.SAVINGS, BigDecimal.ZERO);
        assertNotNull(accountA);
        accountB = accountDAO.createAccount(userBId, AccountType.CURRENT, BigDecimal.ZERO);
        assertNotNull(accountB);
    }

    @AfterAll
    public static void tearDown() {
        DBConnection.clearThreadDatabasePath();
        try {
            if (testDbPath != null && Files.exists(testDbPath)) {
                Files.deleteIfExists(testDbPath);
            }
        } catch (Exception ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("P0-1: Double-Entry Balanced Transfer Verification")
    public void testBalancedTransferDoubleEntry() {
        // Initial deposit to A
        TransactionExecutionResult depRes = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("1000.00"), "Initial deposit", "Salary", "DEP-INIT"
        );
        assertTrue(depRes.isSuccess());

        // Transfer 400.00 from A to B
        TransactionExecutionResult xferRes = ledgerDAO.executeTransfer(
                userAId, accountA.getId(), accountB.getAccountNumber(), new BigDecimal("400.00"),
                "Rent split", "Transfer", "XFER-BAL-1"
        );
        assertTrue(xferRes.isSuccess());
        SimulatorTransaction tx = xferRes.getTransaction();
        assertNotNull(tx);

        List<LedgerEntry> entries = ledgerDAO.getLedgerEntriesForTransaction(tx.getId());
        assertEquals(2, entries.size(), "Transfer must generate exactly 2 ledger entries");

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry e : entries) {
            if (e.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(e.getAmount());
                assertEquals(accountA.getId(), e.getAccountId(), "Debit must target source account");
            } else if (e.getEntryType() == EntryType.CREDIT) {
                totalCredits = totalCredits.add(e.getAmount());
                assertEquals(accountB.getId(), e.getAccountId(), "Credit must target destination account");
            }
        }

        assertEquals(0, totalDebits.compareTo(totalCredits), "Total Debits must strictly equal Total Credits");
        assertEquals(0, new BigDecimal("400.00").compareTo(totalDebits));

        // Reconciled balances check
        Account refA = accountDAO.getAccountById(accountA.getId());
        Account refB = accountDAO.getAccountById(accountB.getId());
        assertEquals(0, refA.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountA.getId())));
        assertEquals(0, refB.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountB.getId())));
    }

    @Test
    @Order(2)
    @DisplayName("P0-2: Concurrent Overdraft Prevention via Isolated Threads")
    public void testConcurrentWithdrawalsOverdraftPrevention() throws Exception {
        // Current balance of accountA is 600.00 (1000 - 400)
        Account currentA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("600.00"), currentA.getBalance());

        // Try to withdraw 500.00 concurrently from 2 threads.
        // Total balance is 600.00, so exactly ONE thread must succeed and ONE must fail.
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> messages = Collections.synchronizedList(new ArrayList<>());

        String dbPathStr = testDbPath.toString();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                // Ensure thread uses the isolated test DB
                DBConnection.setThreadDatabasePath(dbPathStr);
                try {
                    startGate.await();
                    TransactionExecutionResult res = ledgerDAO.executeWithdrawal(
                            userAId, accountA.getId(), new BigDecimal("500.00"),
                            "Concurrent withdrawal attempt " + idx, "ATM", "CONCURR-WTH-" + idx
                    );
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        messages.add(res.getMessage());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    messages.add(e.getMessage());
                } finally {
                    DBConnection.clearThreadDatabasePath();
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Let all threads fire simultaneously
        endGate.await();
        executor.shutdown();

        // Exactly one must succeed and one must fail with insufficient funds
        assertEquals(1, successCount.get(), "Exactly one concurrent withdrawal must succeed");
        assertEquals(1, failureCount.get(), "Second concurrent withdrawal must be rejected");

        // Verify account balance is non-negative and equals 100.00
        Account finalA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("100.00"), finalA.getBalance(), "Balance must be exactly 100.00");
        assertTrue(finalA.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Account must never have negative balance");
        assertEquals(0, finalA.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountA.getId())),
                "Account balance must match ledger reconciled balance");
    }

    @Test
    @Order(3)
    @DisplayName("P0-3: Idempotency Validation (Replay & Invariant Protection)")
    public void testIdempotencyReplayAndSafety() {
        String key = "IDEMP-TEST-KEY-999";

        // First deposit: 250.00
        TransactionExecutionResult res1 = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("250.00"), "Bonus", "Salary", key
        );
        assertTrue(res1.isSuccess());
        assertEquals(new BigDecimal("350.00"), res1.getUpdatedBalance());

        // Repeated identical request with same key
        TransactionExecutionResult res2 = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("250.00"), "Bonus", "Salary", key
        );
        assertTrue(res2.isSuccess());
        assertEquals("Transaction already processed.", res2.getMessage());
        assertEquals(res1.getTransaction().getId(), res2.getTransaction().getId());

        // Ensure balance did not increment a second time
        Account accA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("350.00"), accA.getBalance(), "Balance must not increase on duplicate idempotency key");
    }

    @Test
    @Order(4)
    @DisplayName("P0-4: Transaction PIN Lockout Protection (Max 3 Attempts & 15m Freeze)")
    public void testPinLockoutVerification() {
        // User A's PIN is "4321"
        // Attempt 1: wrong PIN
        PinVerificationResult r1 = userDAO.verifyTransactionPin(userAId, "0000");
        assertFalse(r1.isSuccess());
        assertFalse(r1.isLocked());
        assertEquals(2, r1.getRemainingAttempts());

        // Attempt 2: wrong PIN
        PinVerificationResult r2 = userDAO.verifyTransactionPin(userAId, "1111");
        assertFalse(r2.isSuccess());
        assertFalse(r2.isLocked());
        assertEquals(1, r2.getRemainingAttempts());

        // Attempt 3: wrong PIN -> must lock
        PinVerificationResult r3 = userDAO.verifyTransactionPin(userAId, "2222");
        assertFalse(r3.isSuccess());
        assertTrue(r3.isLocked());
        assertEquals(0, r3.getRemainingAttempts());

        // Attempt 4 with CORRECT PIN while locked -> must STILL BE LOCKED
        PinVerificationResult r4 = userDAO.verifyTransactionPin(userAId, "4321");
        assertFalse(r4.isSuccess());
        assertTrue(r4.isLocked());
        assertTrue(r4.getMessage().contains("locked"));

        // Reset for clean teardown
        userDAO.resetPinAttempts(userAId);
        PinVerificationResult r5 = userDAO.verifyTransactionPin(userAId, "4321");
        assertTrue(r5.isSuccess());
    }

    @Test
    @Order(5)
    @DisplayName("P0-5: Cross-User Authorization Enforcement")
    public void testCrossUserAccountProtection() {
        // User B attempts to withdraw from User A's account
        TransactionExecutionResult unauthWth = ledgerDAO.executeWithdrawal(
                userBId, accountA.getId(), new BigDecimal("50.00"), "Theft attempt", "Other", "THEFT-WTH"
        );
        assertFalse(unauthWth.isSuccess());
        assertTrue(unauthWth.getMessage().contains("denied") || unauthWth.getMessage().contains("not found"));

        // User B attempts to transfer FROM User A's account
        TransactionExecutionResult unauthXfer = ledgerDAO.executeTransfer(
                userBId, accountA.getId(), accountB.getAccountNumber(), new BigDecimal("50.00"),
                "Theft transfer", "Transfer", "THEFT-XFER"
        );
        assertFalse(unauthXfer.isSuccess());
        assertTrue(unauthXfer.getMessage().contains("denied") || unauthXfer.getMessage().contains("not found"));
    }
}

