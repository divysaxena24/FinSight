package com.finsight.engine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Service adapting persisted user transactions into FinSight ParsedTransaction models
 * for historical spending baselines and intelligence calculation.
 */
public class HistoricalTransactionAdapter {

    private final TransactionDAO transactionDAO;

    public HistoricalTransactionAdapter() {
        this(new TransactionDAO());
    }

    public HistoricalTransactionAdapter(TransactionDAO transactionDAO) {
        this.transactionDAO = Objects.requireNonNull(transactionDAO, "transactionDAO must not be null");
    }

    /**
     * Retrieves a user's prior transaction history and converts them into ParsedTransaction instances.
     * Enforces user isolation strictly via the DAO.
     */
    public List<ParsedTransaction> getHistoricalParsedTransactions(int userId) {
        List<Transaction> dbList = transactionDAO.getTransactionsByUser(userId);
        if (dbList == null || dbList.isEmpty()) {
            return new ArrayList<>();
        }

        List<ParsedTransaction> result = new ArrayList<>();
        for (Transaction dbTx : dbList) {
            if (dbTx == null) continue;

            LocalDate date = null;
            if (dbTx.getTransactionDate() != null) {
                date = dbTx.getTransactionDate().toLocalDate();
            } else {
                date = LocalDate.now();
            }

            BigDecimal amount = BigDecimal.valueOf(dbTx.getAmount());
            String typeStr = dbTx.getType() != null ? dbTx.getType().trim().toUpperCase() : "EXPENSE";

            TransactionType type = TransactionType.EXPENSE;
            boolean isRefund = false;

            if ("INCOME".equals(typeStr)) {
                type = TransactionType.INCOME;
                String desc = dbTx.getDescription() != null ? dbTx.getDescription().toUpperCase() : "";
                if (desc.contains("REFUND") || desc.contains("REVERSAL") || desc.contains("REV-")) {
                    isRefund = true;
                }
            } else if ("TRANSFER".equals(typeStr)) {
                type = TransactionType.TRANSFER;
            }

            BigDecimal debit = (type == TransactionType.EXPENSE) ? amount : null;
            BigDecimal credit = (type == TransactionType.INCOME) ? amount : null;

            ParsedTransaction parsed = new ParsedTransaction(
                    "hist-" + dbTx.getId(),
                    date,
                    dbTx.getDescription() != null ? dbTx.getDescription() : "",
                    debit,
                    credit,
                    amount,
                    type,
                    null,
                    null, // Merchant can be normalized if desired
                    dbTx.getCategory() != null ? dbTx.getCategory() : "Other",
                    BigDecimal.ONE,
                    "DATABASE",
                    "HISTORICAL_RECORD",
                    false,
                    false,
                    false,
                    isRefund,
                    true // Manually established category from DB
            );

            result.add(parsed);
        }

        return result;
    }
}

