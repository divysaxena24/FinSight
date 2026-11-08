package com.finsight.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.expense.model.Transaction;
import com.finsight.engine.StatementAnalysisResult.Status;
import com.finsight.engine.analytics.AnalyticsCalculator;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;
import com.finsight.engine.pdf.PdfExtractionResult;
import com.finsight.engine.service.HistoricalTransactionAdapter;
import com.finsight.engine.service.StatementImportService;
import com.finsight.engine.service.StatementImportService.ImportResult;
import com.finsight.engine.staging.StagingSession;
import com.finsight.engine.staging.StagingSession.ImportStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Step 12: Production Hardening & Financial Integrity Tests")
public class ProductionHardeningAndIntegrityTest {

    private StatementAnalysisResult createDummyResult(List<ParsedTransaction> txs) {
        FinancialStatementReport report = new AnalyticsCalculator().calculateReport(txs);
        return new StatementAnalysisResult(
                Status.SUCCESS,
                true,
                PdfExtractionResult.Status.SUCCESS,
                com.finsight.engine.parser.StatementParseResult.Status.SUCCESS,
                "SYNTH_FORMAT",
                "Synthetic Bank",
                BigDecimal.ONE,
                report,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                txs.size(),
                0,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                report.getEffectiveStartDate(),
                report.getEffectiveEndDate(),
                null,
                1,
                false
        );
    }

    private ParsedTransaction createTx(String id, LocalDate date, String desc, BigDecimal amt, TransactionType type, boolean isRefund) {
        return new ParsedTransaction(
                id, date, desc,
                type == TransactionType.EXPENSE ? amt : null,
                type == TransactionType.INCOME ? amt : null,
                amt, type, null, null, "Shopping",
                BigDecimal.ONE, desc, "BANK",
                false, false, false, isRefund, false
        );
    }

    @Test
    @DisplayName("1. Concurrent import protection: exactly one thread claims and executes import")
    void testConcurrentImportProtection() throws Exception {
        int userId = 100;
        List<ParsedTransaction> txs = List.of(
                createTx("t1", LocalDate.of(2026, 3, 1), "Store A", new BigDecimal("100.00"), TransactionType.EXPENSE, false)
        );

        StatementAnalysisResult result = createDummyResult(txs);
        StagingSession session = new StagingSession(userId, result, 3600);

        AtomicInteger dbBatchInsertCalls = new AtomicInteger(0);
        com.expense.dao.TransactionDAO mockDao = new com.expense.dao.TransactionDAO() {
            @Override
            public List<Transaction> getTransactionsByUser(int uId) {
                return Collections.emptyList();
            }

            @Override
            public boolean batchImportTransactions(int uId, List<Transaction> transactions) {
                // Simulate slow database transaction
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                dbBatchInsertCalls.incrementAndGet();
                return true;
            }
        };

        StatementImportService importService = new StatementImportService(mockDao);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<ImportResult>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await(); // Simultaneous trigger
                return importService.importStagedTransactions(session, userId);
            }));
        }

        startLatch.countDown(); // Launch all threads at the exact same instant

        int successCount = 0;
        int failureCount = 0;
        for (Future<ImportResult> f : futures) {
            ImportResult res = f.get();
            if (res.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        executor.shutdown();

        // Exactly one thread succeeds!
        assertEquals(1, successCount);
        assertEquals(numThreads - 1, failureCount);
        // Database batch import executed exactly once!
        assertEquals(1, dbBatchInsertCalls.get());
        assertEquals(ImportStatus.IMPORTED, session.getImportStatus());
    }

    @Test
    @DisplayName("2. Safe rollback on database failure allows retry")
    void testRollbackReleasesImportClaim() {
        int userId = 101;
        List<ParsedTransaction> txs = List.of(
                createTx("t1", LocalDate.of(2026, 3, 1), "Store A", new BigDecimal("100.00"), TransactionType.EXPENSE, false)
        );

        StatementAnalysisResult result = createDummyResult(txs);
        StagingSession session = new StagingSession(userId, result, 3600);

        // DAO that fails first attempt, succeeds second attempt
        AtomicInteger attempt = new AtomicInteger(0);
        com.expense.dao.TransactionDAO mockDao = new com.expense.dao.TransactionDAO() {
            @Override
            public List<Transaction> getTransactionsByUser(int uId) {
                return Collections.emptyList();
            }

            @Override
            public boolean batchImportTransactions(int uId, List<Transaction> transactions) {
                return attempt.incrementAndGet() > 1; // Fails on attempt 1, succeeds on attempt 2
            }
        };

        StatementImportService importService = new StatementImportService(mockDao);

        // Attempt 1: Fails
        ImportResult res1 = importService.importStagedTransactions(session, userId);
        assertFalse(res1.isSuccess());
        // Claim was released back to PENDING_REVIEW!
        assertEquals(ImportStatus.PENDING_REVIEW, session.getImportStatus());

        // Attempt 2: Retry succeeds
        ImportResult res2 = importService.importStagedTransactions(session, userId);
        assertTrue(res2.isSuccess());
        assertEquals(ImportStatus.IMPORTED, session.getImportStatus());
    }

    @Test
    @DisplayName("3. Refund persistence and historical reconstruction round-trip")
    void testRefundPersistenceAndReconstructionRoundTrip() {
        int userId = 102;
        // Parsed refund transaction: Income of 500.00 acting as refund against Shopping
        ParsedTransaction parsedRefund = createTx("ref-1", LocalDate.of(2026, 3, 15), "AMAZON RETURN", new BigDecimal("500.00"), TransactionType.INCOME, true);

        StatementAnalysisResult result = createDummyResult(List.of(parsedRefund));
        StagingSession session = new StagingSession(userId, result, 3600);

        List<Transaction> dbStorage = new ArrayList<>();
        com.expense.dao.TransactionDAO mockDao = new com.expense.dao.TransactionDAO() {
            @Override
            public List<Transaction> getTransactionsByUser(int uId) {
                return new ArrayList<>(dbStorage);
            }

            @Override
            public boolean batchImportTransactions(int uId, List<Transaction> transactions) {
                int idCounter = 1;
                for (Transaction t : transactions) {
                    t.setId(idCounter++);
                    dbStorage.add(t);
                }
                return true;
            }
        };

        StatementImportService importService = new StatementImportService(mockDao);
        ImportResult importResult = importService.importStagedTransactions(session, userId);
        assertTrue(importResult.isSuccess());
        assertEquals(1, importResult.getImportedCount());

        // Verify description was tagged with [Refund] in SQLite
        assertEquals(1, dbStorage.size());
        assertEquals("[Refund] AMAZON RETURN", dbStorage.get(0).getDescription());
        assertEquals("Income", dbStorage.get(0).getType());

        // Historical reconstruction via HistoricalTransactionAdapter
        HistoricalTransactionAdapter adapter = new HistoricalTransactionAdapter(mockDao);
        List<ParsedTransaction> reconstructed = adapter.getHistoricalParsedTransactions(userId);

        assertEquals(1, reconstructed.size());
        ParsedTransaction rTx = reconstructed.get(0);
        // Semantics verification: isRefundOrReversal MUST be true!
        assertTrue(rTx.isRefundOrReversal());
        assertEquals(TransactionType.INCOME, rTx.getTransactionType());
        assertEquals(new BigDecimal("500.0"), rTx.getAmount());

        // Analytics calculation verification: Refund offsets expenses, does NOT inflate net income!
        List<ParsedTransaction> fullLedger = List.of(
                createTx("exp-1", LocalDate.of(2026, 3, 10), "Amazon Store", new BigDecimal("1200.00"), TransactionType.EXPENSE, false),
                rTx
        );

        FinancialStatementReport report = new AnalyticsCalculator().calculateReport(fullLedger);
        assertEquals(new BigDecimal("1200.00"), report.getSummary().getGrossExpenses());
        assertEquals(new BigDecimal("500.00"), report.getSummary().getTotalRefunds());
        assertEquals(new BigDecimal("700.00"), report.getSummary().getNetExpenses());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getSummary().getTotalIncome()); // Total income remains 0.00, refund did not become income!
    }

    @Test
    @DisplayName("4. Transfer handling verification: Excluded from income and expense import")
    void testTransferHandlingVerification() {
        int userId = 103;
        ParsedTransaction transferTx = new ParsedTransaction(
                "trf-1", LocalDate.of(2026, 3, 1), "TRF TO SAVINGS",
                new BigDecimal("25000.00"), null, new BigDecimal("25000.00"),
                TransactionType.TRANSFER, null, "Self", "Transfers",
                BigDecimal.ONE, "TRF TO SAVINGS", "BANK",
                false, false, false, false, false
        );

        StatementAnalysisResult result = createDummyResult(List.of(transferTx));
        StagingSession session = new StagingSession(userId, result, 3600);

        List<Transaction> dbStorage = new ArrayList<>();
        com.expense.dao.TransactionDAO mockDao = new com.expense.dao.TransactionDAO() {
            @Override
            public List<Transaction> getTransactionsByUser(int uId) {
                return dbStorage;
            }

            @Override
            public boolean batchImportTransactions(int uId, List<Transaction> transactions) {
                dbStorage.addAll(transactions);
                return true;
            }
        };

        StatementImportService importService = new StatementImportService(mockDao);
        ImportResult importResult = importService.importStagedTransactions(session, userId);

        assertTrue(importResult.isSuccess());
        assertEquals(0, importResult.getImportedCount());
        assertEquals(1, importResult.getSkippedCount());
        assertTrue(importResult.getWarnings().stream().anyMatch(w -> w.contains("Excluded account transfer")));
        assertEquals(0, dbStorage.size()); // Zero transfers entered SQLite expense/income ledger
    }
}

