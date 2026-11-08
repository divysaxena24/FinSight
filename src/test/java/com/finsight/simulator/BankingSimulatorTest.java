package com.finsight.simulator;

import com.expense.dao.UserDAO;
import com.expense.dao.UserDAO.PinVerificationResult;
import com.expense.model.User;
import com.expense.util.DBConnection;
import com.finsight.engine.insights.BudgetRecommendation;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.IntelligenceDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.dao.LedgerDAO.TransactionExecutionResult;
import com.finsight.simulator.model.*;
import com.finsight.simulator.model.Account.AccountType;
import com.finsight.simulator.model.LedgerEntry.EntryType;
import com.finsight.simulator.model.SimulatorTransaction.TransactionType;
import com.finsight.simulator.service.FinancialIntelligenceService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BankingSimulatorTest {

    private static Path testDbPath;
    private static UserDAO userDAO;
    private static AccountDAO accountDAO;
    private static LedgerDAO ledgerDAO;
    private static IntelligenceDAO intelligenceDAO;
    private static FinancialIntelligenceService intelligenceService;

    private static int userAId;
    private static int userBId;
    private static Account accountA;
    private static Account accountB;

    @BeforeAll
    public static void setUpAll() throws Exception {
        Path testDir = Paths.get("target", "test-data");
        Files.createDirectories(testDir);
        testDbPath = testDir.resolve("simulator_test_" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(testDbPath.toString());

        // Initialize schema by grabbing a connection
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Test database connection must be valid");
        }

        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
        ledgerDAO = new LedgerDAO();
        intelligenceDAO = new IntelligenceDAO();
        intelligenceService = new FinancialIntelligenceService(ledgerDAO, accountDAO, intelligenceDAO);
    }

    @AfterAll
    public static void tearDownAll() {
        DBConnection.clearThreadDatabasePath();
        try {
            if (testDbPath != null && Files.exists(testDbPath)) {
                Files.deleteIfExists(testDbPath);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("User Registration with Phone and Transaction PIN")
    public void testUserRegistrationWithPhoneAndPin() {
        // Register User A
        User userA = new User();
        userA.setName("Alice Banker");
        userA.setEmail("alice@finsight.local");
        userA.setPassword("AliceSecure123!");
        userA.setPhone("9876543210");
        userA.setTransactionPin("1234");

        boolean regA = userDAO.registerUser(userA);
        assertTrue(regA, "Registration for Alice should succeed");

        User fetchedA = userDAO.getUserByEmail("alice@finsight.local");
        assertNotNull(fetchedA);
        userAId = fetchedA.getId();
        assertEquals("9876543210", fetchedA.getPhone());
        assertTrue(userDAO.getUserById(userAId).hasTransactionPin(), "Alice should have a registered transaction PIN");

        // Register User B
        User userB = new User();
        userB.setName("Bob Banker");
        userB.setEmail("bob@finsight.local");
        userB.setPassword("BobSecure123!");
        userB.setPhone("9123456789");
        userB.setTransactionPin("4321");

        boolean regB = userDAO.registerUser(userB);
        assertTrue(regB, "Registration for Bob should succeed");

        User fetchedB = userDAO.getUserByEmail("bob@finsight.local");
        assertNotNull(fetchedB);
        userBId = fetchedB.getId();
        assertEquals("9123456789", fetchedB.getPhone());
        assertTrue(userDAO.getUserById(userBId).hasTransactionPin(), "Bob should have a registered transaction PIN");
    }

    @Test
    @Order(2)
    @DisplayName("Transaction PIN Verification, Failure and Lockout Protection")
    public void testPinVerificationAndLockout() {
        // Correct PIN
        PinVerificationResult res1 = userDAO.verifyTransactionPin(userAId, "1234");
        assertTrue(res1.isSuccess(), "Valid PIN must verify");

        // Incorrect PIN attempts
        PinVerificationResult fail1 = userDAO.verifyTransactionPin(userAId, "9999");
        assertFalse(fail1.isSuccess());
        assertEquals(2, fail1.getRemainingAttempts());

        PinVerificationResult fail2 = userDAO.verifyTransactionPin(userAId, "9998");
        assertFalse(fail2.isSuccess());
        assertEquals(1, fail2.getRemainingAttempts());

        PinVerificationResult fail3 = userDAO.verifyTransactionPin(userAId, "9997");
        assertFalse(fail3.isSuccess());
        assertTrue(fail3.isLocked(), "Alice's PIN should be locked after 3 failed attempts");

        // Check locked status
        User lockedAlice = userDAO.getUserById(userAId);
        assertNotNull(lockedAlice);
        assertTrue(lockedAlice.isPinLocked(), "Alice model isPinLocked should return true");

        // Verification must be rejected even with the correct PIN while locked
        PinVerificationResult whileLocked = userDAO.verifyTransactionPin(userAId, "1234");
        assertFalse(whileLocked.isSuccess(), "Even correct PIN must fail while locked");
        assertTrue(whileLocked.isLocked());

        // Reset lockout for further tests
        userDAO.resetPinAttempts(userAId);
        User unlockedAlice = userDAO.getUserById(userAId);
        assertFalse(unlockedAlice.isPinLocked(), "Alice's PIN should no longer be locked after reset");

        PinVerificationResult afterReset = userDAO.verifyTransactionPin(userAId, "1234");
        assertTrue(afterReset.isSuccess(), "Alice's PIN should verify after reset");
    }

    @Test
    @Order(3)
    @DisplayName("Simulated Bank Account Creation and Format Verification")
    public void testAccountCreation() {
        accountA = accountDAO.createAccount(userAId, AccountType.SAVINGS, BigDecimal.ZERO);
        assertNotNull(accountA, "Account A should be created");
        assertNotNull(accountA.getAccountNumber(), "Account number must not be null");
        assertTrue(accountA.getAccountNumber().startsWith("FS-"), "Account number must start with FS-");
        assertEquals(13, accountA.getAccountNumber().length(), "Account number FS- + 10 digits = 13 chars");
        assertEquals(0, new BigDecimal("0.00").compareTo(accountA.getBalance()));
        assertEquals(AccountType.SAVINGS, accountA.getAccountType());

        accountB = accountDAO.createAccount(userBId, AccountType.CURRENT, BigDecimal.ZERO);
        assertNotNull(accountB, "Account B should be created");
        assertTrue(accountB.getAccountNumber().startsWith("FS-"));
        assertEquals(AccountType.CURRENT, accountB.getAccountType());

        List<Account> aliceAccounts = accountDAO.getAccountsByUserId(userAId);
        assertEquals(1, aliceAccounts.size());
        assertEquals(accountA.getAccountNumber(), aliceAccounts.get(0).getAccountNumber());
    }

    @Test
    @Order(4)
    @DisplayName("Deposit Execution and Ledger Entry Verification")
    public void testDepositOperation() {
        BigDecimal depositAmount = new BigDecimal("50000.00");
        String idemKey = "DEP-" + System.currentTimeMillis();

        TransactionExecutionResult result = ledgerDAO.executeDeposit(
                userAId,
                accountA.getId(),
                depositAmount,
                "Initial Salary Deposit",
                "Salary",
                idemKey
        );

        assertTrue(result.isSuccess(), "Deposit transaction should succeed");
        SimulatorTransaction tx = result.getTransaction();
        assertNotNull(tx, "Deposit transaction should be recorded");
        assertEquals(TransactionType.DEPOSIT, tx.getTransactionType());
        assertEquals(SimulatorTransaction.TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(depositAmount, tx.getAmount());

        // Refresh account and verify balance
        Account refreshedA = accountDAO.getAccountById(accountA.getId());
        assertEquals(depositAmount, refreshedA.getBalance(), "Account balance should be updated to 50000.00");

        // Check ledger entries (Balanced double-entry: Debit Clearing, Credit User)
        List<LedgerEntry> entries = ledgerDAO.getLedgerEntriesForTransaction(tx.getId());
        assertEquals(2, entries.size(), "Deposit should generate balanced double-entry (Debit Clearing, Credit User)");
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            if (e.getEntryType() == EntryType.DEBIT) totalDebits = totalDebits.add(e.getAmount());
            if (e.getEntryType() == EntryType.CREDIT) totalCredits = totalCredits.add(e.getAmount());
        }
        assertEquals(0, totalDebits.compareTo(totalCredits), "Total debits must equal total credits");
        assertEquals(depositAmount, totalCredits);

        // Check reconciled balance
        BigDecimal reconciled = ledgerDAO.calculateReconciledBalance(accountA.getId());
        assertEquals(0, refreshedA.getBalance().compareTo(reconciled), "Reconciled balance from ledger must match account balance");
    }

    @Test
    @Order(5)
    @DisplayName("Withdrawal Execution with Valid PIN, Insufficient Funds, and Overdraft Rejection")
    public void testWithdrawalOperation() {
        // Attempt withdrawal exceeding balance (Overdraft prevention)
        BigDecimal overdraftAmount = new BigDecimal("60000.00");
        TransactionExecutionResult overdraftRes = ledgerDAO.executeWithdrawal(
                userAId,
                accountA.getId(),
                overdraftAmount,
                "Excessive ATM Withdrawal",
                "ATM",
                "WDL-OVERDRAFT"
        );
        assertFalse(overdraftRes.isSuccess(), "Overdraft withdrawal must fail");
        assertTrue(overdraftRes.getMessage().toLowerCase().contains("insufficient"), "Must fail with insufficient balance");

        // Successful withdrawal with valid PIN
        assertTrue(userDAO.verifyTransactionPin(userAId, "1234").isSuccess(), "Valid PIN must verify");
        BigDecimal withdrawAmount = new BigDecimal("5000.00");
        TransactionExecutionResult wRes = ledgerDAO.executeWithdrawal(
                userAId,
                accountA.getId(),
                withdrawAmount,
                "ATM Cash Withdrawal",
                "ATM",
                "WDL-SUCCESS"
        );

        assertTrue(wRes.isSuccess());
        SimulatorTransaction wTx = wRes.getTransaction();
        assertNotNull(wTx);
        assertEquals(TransactionType.WITHDRAWAL, wTx.getTransactionType());

        Account refreshedA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("45000.00"), refreshedA.getBalance(), "Balance should decrease to 45000.00");

        // Check ledger entries (Balanced double-entry: Debit User, Credit Clearing)
        List<LedgerEntry> entries = ledgerDAO.getLedgerEntriesForTransaction(wTx.getId());
        assertEquals(2, entries.size(), "Withdrawal should generate balanced double-entry (Debit User, Credit Clearing)");
        BigDecimal wDebits = BigDecimal.ZERO;
        BigDecimal wCredits = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            if (e.getEntryType() == EntryType.DEBIT) wDebits = wDebits.add(e.getAmount());
            if (e.getEntryType() == EntryType.CREDIT) wCredits = wCredits.add(e.getAmount());
        }
        assertEquals(0, wDebits.compareTo(wCredits), "Total debits must equal total credits");
        assertEquals(withdrawAmount, wDebits);

        // Check reconciled balance
        BigDecimal reconciled = ledgerDAO.calculateReconciledBalance(accountA.getId());
        assertEquals(0, refreshedA.getBalance().compareTo(reconciled), "Reconciled balance must match after withdrawal");
    }

    @Test
    @Order(6)
    @DisplayName("Transfer Execution (Alice to Bob): Double-Entry Atomic Balance & Reconciled Invariants")
    public void testTransferOperation() {
        BigDecimal transferAmount = new BigDecimal("2000.00");
        String idemKey = "XFER-ALICE-BOB-01";

        TransactionExecutionResult xRes = ledgerDAO.executeTransfer(
                userAId,
                accountA.getId(),
                accountB.getAccountNumber(),
                transferAmount,
                "Consulting payment for Bob",
                "Transfer",
                idemKey
        );

        assertTrue(xRes.isSuccess());
        SimulatorTransaction xTx = xRes.getTransaction();
        assertNotNull(xTx);
        assertEquals(TransactionType.TRANSFER, xTx.getTransactionType());
        assertEquals(transferAmount, xTx.getAmount());

        // Verify Account A balance: 45000.00 - 2000.00 = 43000.00
        Account refreshedA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("43000.00"), refreshedA.getBalance());

        // Verify Account B balance: 0.00 + 2000.00 = 2000.00
        Account refreshedB = accountDAO.getAccountById(accountB.getId());
        assertEquals(new BigDecimal("2000.00"), refreshedB.getBalance());

        // Strict double-entry ledger verification:
        List<LedgerEntry> entries = ledgerDAO.getLedgerEntriesForTransaction(xTx.getId());
        assertEquals(2, entries.size(), "Transfer must generate exactly 2 ledger entries");

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
                assertEquals(accountA.getId(), entry.getAccountId(), "Debit entry must belong to source account");
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                totalCredits = totalCredits.add(entry.getAmount());
                assertEquals(accountB.getId(), entry.getAccountId(), "Credit entry must belong to recipient account");
            }
        }

        assertEquals(0, totalDebits.compareTo(totalCredits), "Ledger invariant violated: Debits must strictly equal Credits");
        assertEquals(0, transferAmount.compareTo(totalDebits));

        // Verify both accounts reconcile independently
        assertEquals(0, refreshedA.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountA.getId())));
        assertEquals(0, refreshedB.getBalance().compareTo(ledgerDAO.calculateReconciledBalance(accountB.getId())));

        // Test Idempotency: Repeating same transfer with same key must return existing transaction without double deducting
        TransactionExecutionResult dupRes = ledgerDAO.executeTransfer(
                userAId,
                accountA.getId(),
                accountB.getAccountNumber(),
                transferAmount,
                "Duplicate transfer attempt",
                "Transfer",
                idemKey
        );
        assertTrue(dupRes.isSuccess());
        assertEquals(xTx.getId(), dupRes.getTransaction().getId(), "Duplicate request with same idempotency key must return existing tx");
        Account unchangedA = accountDAO.getAccountById(accountA.getId());
        assertEquals(new BigDecimal("43000.00"), unchangedA.getBalance(), "Balance must not change on duplicate idempotency key");
    }

    @Test
    @Order(7)
    @DisplayName("Self-Transfer and Negative Amount Rejection")
    public void testTransferEdgeCases() {
        // Self-transfer
        TransactionExecutionResult selfRes = ledgerDAO.executeTransfer(
                userAId,
                accountA.getId(),
                accountA.getAccountNumber(),
                new BigDecimal("100.00"),
                "Self transfer attempt",
                "Transfer",
                "SELF-XFER"
        );
        assertFalse(selfRes.isSuccess(), "Transfer to self must be rejected");

        // Zero or negative transfer
        TransactionExecutionResult negRes = ledgerDAO.executeTransfer(
                userAId,
                accountA.getId(),
                accountB.getAccountNumber(),
                new BigDecimal("-50.00"),
                "Negative transfer attempt",
                "Transfer",
                "NEG-XFER"
        );
        assertFalse(negRes.isSuccess(), "Negative transfer amount must be rejected");
    }

    @Test
    @Order(8)
    @DisplayName("Financial Intelligence: Anomaly Detection, Budgets, and Financial Health Scoring")
    public void testFinancialIntelligenceIntegration() {
        // Simulate normal spending pattern for Alice
        for (int i = 0; i < 5; i++) {
            ledgerDAO.executeWithdrawal(
                    userAId,
                    accountA.getId(),
                    new BigDecimal("200.00"),
                    "Coffee and snacks #" + i,
                    "Food & Dining",
                    "ANOMALY-TRAIN-" + i
            );
        }

        // Now withdraw an unusually large amount: 15,000.00 in same category
        TransactionExecutionResult spikeRes = ledgerDAO.executeWithdrawal(
                userAId,
                accountA.getId(),
                new BigDecimal("15000.00"),
                "Luxury Dining Banquet",
                "Food & Dining",
                "ANOMALY-SPIKE"
        );
        assertTrue(spikeRes.isSuccess());

        // Run anomaly evaluation
        intelligenceService.evaluateAndRecordAnomaly(userAId, spikeRes.getTransaction());

        List<AnomalyAlert> alerts = intelligenceDAO.getAlertsForUser(userAId, 10);
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty(), "High spending anomaly should be flagged and recorded");

        // Run budget recommendations
        List<BudgetRecommendation> recs = intelligenceService.getSmartBudgetRecommendations(userAId);
        assertNotNull(recs);

        // Save a budget target
        boolean budgetSaved = intelligenceDAO.saveOrUpdateBudget(userAId, "Food & Dining", new BigDecimal("5000.00"), "2026-11");
        assertTrue(budgetSaved, "Budget limit should be successfully saved");

        List<CategoryBudget> savedBudgets = intelligenceDAO.getBudgetsForMonth(userAId, "2026-11");
        assertFalse(savedBudgets.isEmpty(), "Saved budget should be retrievable");

        // Run Financial Health Scoring
        FinancialHealthReport health = intelligenceService.calculateFinancialHealth(userAId);
        assertNotNull(health);
        assertTrue(health.getScore() >= 0 && health.getScore() <= 100, "Score must be bounded between 0 and 100");
        assertNotNull(health.getStatusCategory());
        assertNotNull(health.getAreasRequiringAttention());
    }
}
