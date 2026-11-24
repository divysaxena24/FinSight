package com.finsight.simulator;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.expense.dao.UserDAO;
import com.expense.model.User;
import com.expense.util.DBConnection;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.dao.LedgerDAO.TransactionExecutionResult;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.Account.AccountType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountLookupAndQRTransferTest {

    private Path tempDb;
    private AccountDAO accountDAO;
    private LedgerDAO ledgerDAO;
    private UserDAO userDAO;

    @BeforeEach
    public void setUp() throws Exception {
        tempDb = Files.createTempFile("finsight-qr-test-", ".db");
        DBConnection.setThreadDatabasePath(tempDb.toString());
        accountDAO = new AccountDAO();
        ledgerDAO = new LedgerDAO();
        userDAO = new UserDAO();
    }

    @AfterEach
    public void tearDown() throws Exception {
        DBConnection.clearThreadDatabasePath();
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
        }
    }

    @Test
    public void testGetOtherActiveAccountsAndLookup() {
        // Create user 1 and user 2
        User user1 = new User(0, "Alice Sender", "alice@example.com", "SecretPass123!");
        user1.setTransactionPin("1234");
        userDAO.registerUserWithResult(user1);
        User dbUser1 = userDAO.getUserByEmail("alice@example.com");

        User user2 = new User(0, "Bob Receiver", "bob@example.com", "SecretPass123!");
        user2.setTransactionPin("5678");
        userDAO.registerUserWithResult(user2);
        User dbUser2 = userDAO.getUserByEmail("bob@example.com");

        // Create accounts for both
        Account accAlice = accountDAO.createAccount(dbUser1.getId(), AccountType.SAVINGS, new BigDecimal("5000.00"));
        Account accBob = accountDAO.createAccount(dbUser2.getId(), AccountType.SAVINGS, new BigDecimal("1000.00"));

        assertNotNull(accAlice);
        assertNotNull(accBob);

        // 1. Test getOtherActiveAccounts for Alice (should list Bob with holderName)
        List<Account> othersForAlice = accountDAO.getOtherActiveAccounts(dbUser1.getId(), 5);
        assertFalse(othersForAlice.isEmpty());
        boolean foundBob = false;
        for (Account a : othersForAlice) {
            if (a.getAccountNumber().equals(accBob.getAccountNumber())) {
                foundBob = true;
                assertEquals("Bob Receiver", a.getHolderName());
            }
        }
        assertTrue(foundBob, "Bob's account should be present in directory for Alice");

        // 2. Lookup Bob's account by account number
        Account lookedUp = accountDAO.getAccountByNumber(accBob.getAccountNumber());
        assertNotNull(lookedUp);
        User holder = userDAO.getUserById(lookedUp.getUserId());
        assertNotNull(holder);
        assertEquals("Bob Receiver", holder.getName());

        // 3. Test Transfer Execution (e.g. triggered via manual or QR flow)
        TransactionExecutionResult xferResult = ledgerDAO.executeTransfer(
                dbUser1.getId(),
                accAlice.getId(),
                accBob.getAccountNumber(),
                new BigDecimal("750.00"),
                "Instant QR Payment for Dinner",
                "Transfer",
                "IDEMP-QR-" + System.currentTimeMillis()
        );

        assertTrue(xferResult.isSuccess(), "Transfer should succeed: " + xferResult.getMessage());
        Account updatedAlice = accountDAO.getAccountById(accAlice.getId());
        Account updatedBob = accountDAO.getAccountById(accBob.getId());

        assertEquals(new BigDecimal("4250.00"), updatedAlice.getBalance());
        assertEquals(new BigDecimal("1750.00"), updatedBob.getBalance());
    }

    @Test
    public void testBudgetLimitRestrictionAndOverrideOnTransfer() {
        // Register sender Alice and recipient Bob
        User userAlice = new User(0, "Alice Budgeter", "alice.budget@example.com", "Password123!");
        userDAO.registerUserWithResult(userAlice);
        User dbAlice = userDAO.getUserByEmail("alice.budget@example.com");

        User userBob = new User(0, "Bob Merchant", "bob.merchant@example.com", "Password123!");
        userDAO.registerUserWithResult(userBob);
        User dbBob = userDAO.getUserByEmail("bob.merchant@example.com");

        Account accAlice = accountDAO.createAccount(dbAlice.getId(), AccountType.SAVINGS, new BigDecimal("10000.00"));
        Account accBob = accountDAO.createAccount(dbBob.getId(), AccountType.SAVINGS, new BigDecimal("500.00"));

        // 1. Set Shopping budget limit of 3,000.00 for current month
        com.finsight.simulator.dao.IntelligenceDAO intelligenceDAO = new com.finsight.simulator.dao.IntelligenceDAO();
        String currentMonth = java.time.YearMonth.now().toString();
        intelligenceDAO.saveOrUpdateBudget(dbAlice.getId(), "Shopping", new BigDecimal("3000.00"), currentMonth);

        // 2. Attempt to transfer 3,500.00 for Shopping without override -> MUST BE RESTRICTED
        TransactionExecutionResult blockedResult = ledgerDAO.executeTransfer(
                dbAlice.getId(),
                accAlice.getId(),
                accBob.getAccountNumber(),
                new BigDecimal("3500.00"),
                "Online Shopping payment",
                "Shopping",
                "IDEMP-BLOCKED-1",
                false
        );

        assertFalse(blockedResult.isSuccess(), "Transfer exceeding budget without override must be blocked");
        assertTrue(blockedResult.getMessage().contains("budget limit exceeded"), "Message should mention budget limit: " + blockedResult.getMessage());

        // Verify balances unchanged
        assertEquals(new BigDecimal("10000.00"), accountDAO.getAccountById(accAlice.getId()).getBalance());
        assertEquals(new BigDecimal("500.00"), accountDAO.getAccountById(accBob.getId()).getBalance());

        // 3. User confirms override -> Transfer MUST SUCCEED
        TransactionExecutionResult allowedResult = ledgerDAO.executeTransfer(
                dbAlice.getId(),
                accAlice.getId(),
                accBob.getAccountNumber(),
                new BigDecimal("3500.00"),
                "Online Shopping payment (Confirmed override)",
                "Shopping",
                "IDEMP-ALLOWED-2",
                true
        );

        assertTrue(allowedResult.isSuccess(), "Transfer with override confirmed should succeed: " + allowedResult.getMessage());
        assertEquals(new BigDecimal("6500.00"), accountDAO.getAccountById(accAlice.getId()).getBalance());
        assertEquals(new BigDecimal("4000.00"), accountDAO.getAccountById(accBob.getId()).getBalance());

        // 4. Verify that this transfer is now tracked as part of monthly Shopping spend
        BigDecimal shoppingSpend = intelligenceDAO.getMonthlyCategorySpend(dbAlice.getId(), "Shopping", currentMonth);
        assertEquals(new BigDecimal("3500.00"), shoppingSpend, "Shopping spend should now record the 3500 transfer");
    }
}
