package com.expense.service;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;

public class TransactionService {

    private final TransactionDAO transactionDAO;
    private final Clock clock;

    public TransactionService() {
        this(new TransactionDAO(), Clock.systemDefaultZone());
    }

    public TransactionService(TransactionDAO transactionDAO) {
        this(transactionDAO, Clock.systemDefaultZone());
    }

    public TransactionService(TransactionDAO transactionDAO, Clock clock) {
        this.transactionDAO = transactionDAO;
        this.clock = (clock != null) ? clock : Clock.systemDefaultZone();
    }

    public static class DateRangeResolution {
        private final String filter;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String errorMessage;

        public DateRangeResolution(String filter, LocalDate startDate, LocalDate endDate, String errorMessage) {
            this.filter = filter;
            this.startDate = startDate;
            this.endDate = endDate;
            this.errorMessage = errorMessage;
        }

        public String getFilter() {
            return filter;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getStartDateString() {
            return (startDate != null) ? startDate.toString() : "";
        }

        public String getEndDateString() {
            return (endDate != null) ? endDate.toString() : "";
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean hasError() {
            return errorMessage != null && !errorMessage.isEmpty();
        }
    }

    public static class DashboardSummary {
        private final double totalIncome;
        private final double totalExpense;
        private final double balance;
        private final int transactionCount;
        private final DateRangeResolution dateRange;

        public DashboardSummary(double totalIncome, double totalExpense, double balance, int transactionCount) {
            this(totalIncome, totalExpense, balance, transactionCount, null);
        }

        public DashboardSummary(double totalIncome, double totalExpense, double balance, int transactionCount, DateRangeResolution dateRange) {
            this.totalIncome = Math.round(totalIncome * 100.0) / 100.0;
            this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
            this.balance = Math.round(balance * 100.0) / 100.0;
            this.transactionCount = transactionCount;
            this.dateRange = dateRange;
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

        public DateRangeResolution getDateRange() {
            return dateRange;
        }
    }

    // Resolves DateRange with deterministic Clock and strict custom validation
    public DateRangeResolution resolveDateRange(int userId, String rangeParam, String customStart, String customEnd) {
        LocalDate today = LocalDate.now(clock);
        String normalizedRange = (rangeParam != null && !rangeParam.trim().isEmpty())
                ? rangeParam.trim().toUpperCase()
                : "1M";

        switch (normalizedRange) {
            case "1M":
                return new DateRangeResolution("1M", today.minusMonths(1), today, null);

            case "6M":
                return new DateRangeResolution("6M", today.minusMonths(6), today, null);

            case "1Y":
                return new DateRangeResolution("1Y", today.minusYears(1), today, null);

            case "ALL":
                String earliest = transactionDAO.getEarliestTransactionDate(userId);
                if (earliest != null && !earliest.trim().isEmpty()) {
                    try {
                        LocalDate minDate = LocalDate.parse(earliest.trim());
                        LocalDate effectiveEnd = minDate.isAfter(today) ? minDate : today;
                        return new DateRangeResolution("ALL", minDate, effectiveEnd, null);
                    } catch (DateTimeParseException e) {
                        return new DateRangeResolution("ALL", today, today, null);
                    }
                } else {
                    // No transactions yet for user
                    return new DateRangeResolution("ALL", today, today, null);
                }

            case "CUSTOM":
                if (customStart == null || customStart.trim().isEmpty() ||
                    customEnd == null || customEnd.trim().isEmpty()) {
                    return new DateRangeResolution("CUSTOM", today.minusMonths(1), today,
                            "Both start date and end date are required for custom range.");
                }

                LocalDate parsedStart;
                LocalDate parsedEnd;
                try {
                    parsedStart = LocalDate.parse(customStart.trim());
                } catch (DateTimeParseException e) {
                    return new DateRangeResolution("CUSTOM", today.minusMonths(1), today,
                            "Invalid start date format. Please use YYYY-MM-DD.");
                }

                try {
                    parsedEnd = LocalDate.parse(customEnd.trim());
                } catch (DateTimeParseException e) {
                    return new DateRangeResolution("CUSTOM", today.minusMonths(1), today,
                            "Invalid end date format. Please use YYYY-MM-DD.");
                }

                if (parsedStart.isAfter(parsedEnd)) {
                    return new DateRangeResolution("CUSTOM", parsedStart, parsedEnd,
                            "Start date cannot be after end date.");
                }

                return new DateRangeResolution("CUSTOM", parsedStart, parsedEnd, null);

            default:
                // Unknown range defaults to 1M
                return new DateRangeResolution("1M", today.minusMonths(1), today, null);
        }
    }

    public static class MonthlyTrendItem {
        private final String key;        // "2026-11"
        private final String label;      // "Nov 2026"
        private final double income;
        private final double expense;

        public MonthlyTrendItem(String key, String label, double income, double expense) {
            this.key = key;
            this.label = label;
            this.income = Math.round(income * 100.0) / 100.0;
            this.expense = Math.round(expense * 100.0) / 100.0;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public double getIncome() {
            return income;
        }

        public double getExpense() {
            return expense;
        }
    }

    public static class MonthlyTrendChartData {
        private final List<String> labels;
        private final List<Double> income;
        private final List<Double> expense;
        private final boolean hasData;

        public MonthlyTrendChartData(List<String> labels, List<Double> income, List<Double> expense, boolean hasData) {
            this.labels = labels;
            this.income = income;
            this.expense = expense;
            this.hasData = hasData;
        }

        public List<String> getLabels() {
            return labels;
        }

        public List<Double> getIncome() {
            return income;
        }

        public List<Double> getExpense() {
            return expense;
        }

        public boolean isHasData() {
            return hasData;
        }
    }

    // Generate continuous monthly trends across resolved date range
    public MonthlyTrendChartData getMonthlyTrendChartData(int userId, DateRangeResolution range) {
        if (range == null || range.hasError() || range.getStartDate() == null || range.getEndDate() == null) {
            return new MonthlyTrendChartData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false);
        }

        LocalDate start = range.getStartDate();
        LocalDate end = range.getEndDate();

        if (start.isAfter(end)) {
            return new MonthlyTrendChartData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false);
        }

        YearMonth startYm = YearMonth.from(start);
        YearMonth endYm = YearMonth.from(end);

        // Fetch raw DB records for the date window
        List<TransactionDAO.MonthlyRecord> dbRecords = transactionDAO.getMonthlyIncomeAndExpenses(
                userId, range.getStartDateString(), range.getEndDateString());

        Map<String, TransactionDAO.MonthlyRecord> recordMap = new HashMap<>();
        if (dbRecords != null) {
            for (TransactionDAO.MonthlyRecord r : dbRecords) {
                recordMap.put(r.getYearMonth(), r);
            }
        }

        List<String> labels = new ArrayList<>();
        List<Double> incomeList = new ArrayList<>();
        List<Double> expenseList = new ArrayList<>();
        boolean hasNonZero = false;

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        YearMonth current = startYm;
        while (!current.isAfter(endYm)) {
            String ymKey = current.toString(); // "YYYY-MM"
            String label = current.format(labelFormatter); // "Jan 2026"
            labels.add(label);

            TransactionDAO.MonthlyRecord rec = recordMap.get(ymKey);
            double inc = (rec != null) ? Math.round(rec.getIncome() * 100.0) / 100.0 : 0.0;
            double exp = (rec != null) ? Math.round(rec.getExpense() * 100.0) / 100.0 : 0.0;

            if (inc > 0 || exp > 0) {
                hasNonZero = true;
            }

            incomeList.add(inc);
            expenseList.add(exp);

            current = current.plusMonths(1);
        }

        return new MonthlyTrendChartData(labels, incomeList, expenseList, hasNonZero);
    }

    // Category breakdown item DTO
    public static class CategoryBreakdownItem {
        private final String category;
        private final double totalExpense;
        private final int transactionCount;
        private final double percentage; // e.g. 35.5

        public CategoryBreakdownItem(String category, double totalExpense, int transactionCount, double percentage) {
            this.category = category;
            this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
            this.transactionCount = transactionCount;
            this.percentage = Math.round(percentage * 10.0) / 10.0;
        }

        public String getCategory() {
            return category;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public double getPercentage() {
            return percentage;
        }
    }

    // Category breakdown chart & list container
    public static class CategoryBreakdownData {
        private final List<CategoryBreakdownItem> items;
        private final List<String> labels;
        private final List<Double> data;
        private final double totalExpenses;
        private final boolean hasData;

        public CategoryBreakdownData(List<CategoryBreakdownItem> items, List<String> labels,
                                    List<Double> data, double totalExpenses, boolean hasData) {
            this.items = items;
            this.labels = labels;
            this.data = data;
            this.totalExpenses = Math.round(totalExpenses * 100.0) / 100.0;
            this.hasData = hasData;
        }

        public List<CategoryBreakdownItem> getItems() {
            return items;
        }

        public List<String> getLabels() {
            return labels;
        }

        public List<Double> getData() {
            return data;
        }

        public double getTotalExpenses() {
            return totalExpenses;
        }

        public boolean isHasData() {
            return hasData;
        }
    }

    // Retrieve category-wise expense breakdown for the resolved date range
    public CategoryBreakdownData getCategoryBreakdownData(int userId, DateRangeResolution range) {
        if (range == null || range.hasError() || range.getStartDate() == null || range.getEndDate() == null) {
            return new CategoryBreakdownData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0.0, false);
        }

        List<TransactionDAO.CategoryExpenseRecord> records = transactionDAO.getCategoryExpensesByDateRange(
                userId, range.getStartDateString(), range.getEndDateString());

        if (records == null || records.isEmpty()) {
            return new CategoryBreakdownData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0.0, false);
        }

        double grandTotal = 0.0;
        for (TransactionDAO.CategoryExpenseRecord r : records) {
            if (r.getTotalExpense() > 0) {
                grandTotal += r.getTotalExpense();
            }
        }

        List<CategoryBreakdownItem> items = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (TransactionDAO.CategoryExpenseRecord r : records) {
            if (r.getTotalExpense() <= 0) {
                continue;
            }
            double roundedAmount = Math.round(r.getTotalExpense() * 100.0) / 100.0;
            double pct = (grandTotal > 0) ? (roundedAmount / grandTotal) * 100.0 : 0.0;
            double roundedPct = Math.round(pct * 10.0) / 10.0;

            String catName = (r.getCategory() == null || r.getCategory().trim().isEmpty())
                    ? "Uncategorized" : r.getCategory().trim();

            items.add(new CategoryBreakdownItem(catName, roundedAmount, r.getCount(), roundedPct));
            labels.add(catName);
            data.add(roundedAmount);
        }

        boolean hasData = !items.isEmpty() && grandTotal > 0;
        return new CategoryBreakdownData(items, labels, data, grandTotal, hasData);
    }

    // Filtered Financial Summary for Dashboard
    public DashboardSummary getFilteredDashboardSummary(int userId, String rangeParam, String customStart, String customEnd) {
        DateRangeResolution range = resolveDateRange(userId, rangeParam, customStart, customEnd);

        if (range.hasError() && "CUSTOM".equals(range.getFilter()) && (range.getStartDate() == null || range.getStartDate().isAfter(range.getEndDate()))) {
            // When custom range is structurally invalid, return zero summary with error
            return new DashboardSummary(0.0, 0.0, 0.0, 0, range);
        }

        TransactionDAO.SummaryRecord record = transactionDAO.getSummaryByDateRange(
                userId, range.getStartDateString(), range.getEndDateString());

        double balance = record.getTotalIncome() - record.getTotalExpense();
        return new DashboardSummary(record.getTotalIncome(), record.getTotalExpense(), balance, record.getCount(), range);
    }

    // Backward-compatible default summary (1M)
    public DashboardSummary getDashboardSummary(int userId) {
        return getFilteredDashboardSummary(userId, "1M", null, null);
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
