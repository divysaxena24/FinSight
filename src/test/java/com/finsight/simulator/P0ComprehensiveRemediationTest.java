package com.finsight.simulator;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.expense.dao.UserDAO;
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
public class P0ComprehensiveRemediationTest {

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
        testDbPath = testDir.resolve("p0_remediation_test_" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbPath.toString());

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Test database connection must be valid");
        }

        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
        ledgerDAO = new LedgerDAO();

        // User A
        User userA = new User();
        userA.setName("Remediation User A");
        userA.setEmail("remediation_a@finsight.test");
        userA.setPassword("PassA123");
        userA.setPhone("9988776655");
        userA.setTransactionPin("1234");
        assertTrue(userDAO.registerUser(userA));
        User loggedA = userDAO.loginUser("remediation_a@finsight.test", "PassA123");
        userAId = loggedA.getId();

        // User B
        User userB = new User();
        userB.setName("Remediation User B");
        userB.setEmail("remediation_b@finsight.test");
        userB.setPassword("PassB123");
        userB.setPhone("9988776644");
        userB.setTransactionPin("5678");
        assertTrue(userDAO.registerUser(userB));
        User loggedB = userDAO.loginUser("remediation_b@finsight.test", "PassB123");
        userBId = loggedB.getId();

        accountA = accountDAO.createAccount(userAId, AccountType.SAVINGS, BigDecimal.ZERO);
        accountB = accountDAO.createAccount(userBId, AccountType.CURRENT, BigDecimal.ZERO);
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
    @DisplayName("FIX 1: Balanced Double-Entry for Deposits, Withdrawals, and Transfers")
    public void testBalancedDoubleEntryAcrossAllOperations() {
        // 1. DEPOSIT: 1000.00
        TransactionExecutionResult depRes = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("1000.00"), "Deposit salary", "Salary", "DEP-BAL-1"
        );
        assertTrue(depRes.isSuccess());
        SimulatorTransaction depTx = depRes.getTransaction();
        List<LedgerEntry> depEntries = ledgerDAO.getLedgerEntriesForTransaction(depTx.getId());
        assertEquals(2, depEntries.size(), "Deposit must create balanced double-entry (Debit Clearing, Credit Customer)");

        BigDecimal depDebits = BigDecimal.ZERO;
        BigDecimal depCredits = BigDecimal.ZERO;
        for (LedgerEntry e : depEntries) {
            if (e.getEntryType() == EntryType.DEBIT) depDebits = depDebits.add(e.getAmount());
            if (e.getEntryType() == EntryType.CREDIT) depCredits = depCredits.add(e.getAmount());
        }
        assertEquals(0, depDebits.compareTo(depCredits), "Deposit total debits must equal total credits");
        assertEquals(0, new BigDecimal("1000.00").compareTo(depDebits));

        // 2. WITHDRAWAL: 300.00
        TransactionExecutionResult wthRes = ledgerDAO.executeWithdrawal(
                userAId, accountA.getId(), new BigDecimal("300.00"), "ATM withdrawal", "Cash", "WTH-BAL-1"
        );
        assertTrue(wthRes.isSuccess());
        SimulatorTransaction wthTx = wthRes.getTransaction();
        List<LedgerEntry> wthEntries = ledgerDAO.getLedgerEntriesForTransaction(wthTx.getId());
        assertEquals(2, wthEntries.size(), "Withdrawal must create balanced double-entry (Debit Customer, Credit Clearing)");

        BigDecimal wthDebits = BigDecimal.ZERO;
        BigDecimal wthCredits = BigDecimal.ZERO;
        for (LedgerEntry e : wthEntries) {
            if (e.getEntryType() == EntryType.DEBIT) wthDebits = wthDebits.add(e.getAmount());
            if (e.getEntryType() == EntryType.CREDIT) wthCredits = wthCredits.add(e.getAmount());
        }
        assertEquals(0, wthDebits.compareTo(wthCredits), "Withdrawal total debits must equal total credits");
        assertEquals(0, new BigDecimal("300.00").compareTo(wthDebits));

        // 3. TRANSFER: 200.00 from A to B
        TransactionExecutionResult xferRes = ledgerDAO.executeTransfer(
                userAId, accountA.getId(), accountB.getAccountNumber(), new BigDecimal("200.00"), "Payment", "Transfer", "XFER-BAL-1"
        );
        assertTrue(xferRes.isSuccess());
        SimulatorTransaction xferTx = xferRes.getTransaction();
        List<LedgerEntry> xferEntries = ledgerDAO.getLedgerEntriesForTransaction(xferTx.getId());
        assertEquals(2, xferEntries.size(), "Transfer must create balanced double-entry (Debit A, Credit B)");

        BigDecimal xferDebits = BigDecimal.ZERO;
        BigDecimal xferCredits = BigDecimal.ZERO;
        for (LedgerEntry e : xferEntries) {
            if (e.getEntryType() == EntryType.DEBIT) xferDebits = xferDebits.add(e.getAmount());
            if (e.getEntryType() == EntryType.CREDIT) xferCredits = xferCredits.add(e.getAmount());
        }
        assertEquals(0, xferDebits.compareTo(xferCredits), "Transfer total debits must equal total credits");
        assertEquals(0, new BigDecimal("200.00").compareTo(xferDebits));

        // 4. Verify customer accounts reconcile
        Account refA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("500.00"), refA.getBalance()); // 1000 - 300 - 200
        assertEquals(0, refA.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountA.getId())));

        Account refB = accountDAO.getAccountById(accountB.getId());
        assertEquals(new BigDecimal("200.00"), refB.getBalance());
        assertEquals(0, refB.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountB.getId())));
    }

    @Test
    @Order(2)
    @DisplayName("FIX 2: Idempotency Payload Validation (Identical Replay vs Conflicting Rejection)")
    public void testIdempotencyPayloadValidation() {
        String key = "IDEMP-TEST-VALIDATION-KEY-1";

        // Initial deposit of 450.00
        TransactionExecutionResult res1 = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("450.00"), "First submission", "Salary", key
        );
        assertTrue(res1.isSuccess());

        // Identical replay with SAME payload: Must succeed with cached result
        TransactionExecutionResult resIdentical = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("450.00"), "Second submission", "Salary", key
        );
        assertTrue(resIdentical.isSuccess());
        assertEquals("Transaction already processed.", resIdentical.getMessage());
        assertEquals(res1.getTransaction().getId(), resIdentical.getTransaction().getId());

        // Conflicting replay with DIFFERENT amount (e.g. 999.00): Must be rejected with Conflict
        TransactionExecutionResult resConflictAmount = ledgerDAO.executeDeposit(
                userAId, accountA.getId(), new BigDecimal("999.00"), "Third submission", "Salary", key
        );
        assertFalse(resConflictAmount.isSuccess(), "Conflicting amount must be rejected");
        assertTrue(resConflictAmount.getMessage().contains("Conflict"));

        // Conflicting replay with DIFFERENT account: Must be rejected with Conflict
        TransactionExecutionResult resConflictAccount = ledgerDAO.executeDeposit(
                userBId, accountB.getId(), new BigDecimal("450.00"), "Fourth submission", "Salary", key
        );
        assertFalse(resConflictAccount.isSuccess(), "Conflicting account must be rejected");
        assertTrue(resConflictAccount.getMessage().contains("Conflict"));
    }

    @Test
    @Order(3)
    @DisplayName("FIX 3: Ledger Immutability & Safe Restrictive Deletion Behavior")
    public void testLedgerImmutabilityAndForeignKeys() throws Exception {
        Account tempAcc = accountDAO.createAccount(userBId, AccountType.SAVINGS, BigDecimal.ZERO);
        assertNotNull(tempAcc);

        ledgerDAO.executeDeposit(userBId, tempAcc.getId(), new BigDecimal("250.00"), "Safe test", "Cash", "TEMP-IMMUT-1");

        // Attempting to delete account with existing transactions directly in SQL must be blocked
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            boolean dropped = false;
            try {
                stmt.execute("DELETE FROM accounts WHERE id = " + tempAcc.getId() + ";");
                dropped = true;
            } catch (Exception e) {
                assertTrue(e.getMessage().toLowerCase().contains("constraint") || e.getMessage().toLowerCase().contains("foreign key"));
            }
            assertFalse(dropped, "Account with financial transaction history must not be deleted due to FK constraints");
        }
    }

    @Test
    @Order(4)
    @DisplayName("FIX 4: SQLite Busy Timeout and High-Contention Multi-Thread Transfers")
    public void testConcurrentTransfersWithBusyTimeout() throws Exception {
        // Deposit 1,200.00 into Account B
        ledgerDAO.executeDeposit(userBId, accountB.getId(), new BigDecimal("1200.00"), "Preload", "Salary", "DEP-CONCURR-PRELOAD");

        // 3 threads transferring 400.00 simultaneously from B to A (Total 1,200.00 == Balance 1,200.00)
        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        String dbPathStr = testDbPath.toString();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                DBConnection.setThreadDatabasePath(dbPathStr);
                try {
                    startGate.await();
                    TransactionExecutionResult res = ledgerDAO.executeTransfer(
                            userBId, accountB.getId(), accountA.getAccountNumber(),
                            new BigDecimal("400.00"), "Transfer " + idx, "Transfer", "CONCURR-BUSY-XFER-" + idx
                    );
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    DBConnection.clearThreadDatabasePath();
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executor.shutdown();

        // Thanks to PRAGMA busy_timeout = 5000 and PRAGMA journal_mode = WAL, all 3 threads serialized cleanly
        assertEquals(3, successCount.get(), "All 3 concurrent transfers should serialize and succeed under busy_timeout");

        Account finalB = accountDAO.getAccountById(accountB.getId());
        assertEquals(new BigDecimal("200.00"), finalB.getBalance()); // (200 initial + 1200 deposit - 1200 transfers)
        assertEquals(0, finalB.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountB.getId())));
    }
}

