package com.expense.service;

import java.sql.Date;
import java.util.List;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;

public class TransactionService {

    private final TransactionDAO transactionDAO;

    public TransactionService() {
        this.transactionDAO = new TransactionDAO();
    }

    public TransactionService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public static class DashboardSummary {
        private final double totalIncome;
        private final double totalExpense;
        private final double balance;
        private final int transactionCount;

        public DashboardSummary(double totalIncome, double totalExpense, double balance, int transactionCount) {
            this.totalIncome = Math.round(totalIncome * 100.0) / 100.0;
            this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
            this.balance = Math.round(balance * 100.0) / 100.0;
            this.transactionCount = transactionCount;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public double getBalance() {
            return balance;
        }

        public int getTransactionCount() {
            return transactionCount;
        }
    }

    public DashboardSummary getDashboardSummary(int userId) {
        double income = transactionDAO.getTotalIncome(userId);
        double expense = transactionDAO.getTotalExpense(userId);
        double balance = income - expense;
        List<Transaction> transactions = transactionDAO.getTransactionsByUser(userId);
        int count = (transactions != null) ? transactions.size() : 0;

        return new DashboardSummary(income, expense, balance, count);
    }

    public List<Transaction> getUserTransactions(int userId) {
        return transactionDAO.getTransactionsByUser(userId);
    }

    public Transaction getTransaction(int transactionId, int userId) {
        return transactionDAO.getTransactionById(transactionId, userId);
    }

    public boolean createTransaction(int userId, String type, String category, double amount, String description, String dateStr) {
        if (userId <= 0 || amount <= 0 || type == null) {
            return false;
        }

        if (!"Income".equalsIgnoreCase(type) && !"Expense".equalsIgnoreCase(type)) {
            return false;
        }

        String normalizedType = "Income".equalsIgnoreCase(type) ? "Income" : "Expense";
        String cleanCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : "General";
        String cleanDesc = (description != null) ? description.trim() : "";

        Date txDate;
        try {
            txDate = (dateStr != null && !dateStr.trim().isEmpty())
                    ? Date.valueOf(dateStr.trim())
                    : new Date(System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            txDate = new Date(System.currentTimeMillis());
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType(normalizedType);
        transaction.setCategory(cleanCategory);
        transaction.setAmount(Math.round(amount * 100.0) / 100.0);
        transaction.setDescription(cleanDesc);
        transaction.setTransactionDate(txDate);

        return transactionDAO.addTransaction(transaction);
    }

    public boolean updateTransaction(int transactionId, int userId, String type, String category, double amount, String description, String dateStr) {
        if (transactionId <= 0 || userId <= 0 || amount <= 0 || type == null) {
            return false;
        }

        if (!"Income".equalsIgnoreCase(type) && !"Expense".equalsIgnoreCase(type)) {
            return false;
        }

        String normalizedType = "Income".equalsIgnoreCase(type) ? "Income" : "Expense";
        String cleanCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : "General";
        String cleanDesc = (description != null) ? description.trim() : "";

        Date txDate;
        try {
            txDate = (dateStr != null && !dateStr.trim().isEmpty())
                    ? Date.valueOf(dateStr.trim())
                    : new Date(System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            txDate = new Date(System.currentTimeMillis());
        }

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setUserId(userId);
        transaction.setType(normalizedType);
        transaction.setCategory(cleanCategory);
        transaction.setAmount(Math.round(amount * 100.0) / 100.0);
        transaction.setDescription(cleanDesc);
        transaction.setTransactionDate(txDate);

        return transactionDAO.updateTransaction(transaction, userId);
    }

    public boolean deleteTransaction(int transactionId, int userId) {
        if (transactionId <= 0 || userId <= 0) {
            return false;
        }
        return transactionDAO.deleteTransaction(transactionId, userId);
    }
}
