package com.finsight.simulator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.finsight.simulator.dao.IntelligenceDAO;

public class BudgetLimitService {

    public static class CheckResult {
        private final boolean allowed;
        private final String message;
        private final BigDecimal limit;
        private final BigDecimal spent;
        private final BigDecimal remaining;

        private CheckResult(boolean allowed, String message, BigDecimal limit,
                            BigDecimal spent, BigDecimal remaining) {
            this.allowed = allowed;
            this.message = message;
            this.limit = limit;
            this.spent = spent;
            this.remaining = remaining;
        }

        public static CheckResult allowed() {
            return new CheckResult(true, null, null, null, null);
        }

        public static CheckResult allowed(BigDecimal limit, BigDecimal spent, BigDecimal remaining) {
            return new CheckResult(true, null, limit, spent, remaining);
        }

        public static CheckResult blocked(BigDecimal limit, BigDecimal spent,
                                          BigDecimal remaining, String category) {
            String message = category + " budget limit exceeded. "
                    + "Monthly limit: ₹" + format(limit) + ". "
                    + "Spent: ₹" + format(spent) + ". "
                    + "Remaining: ₹" + format(remaining) + ".";
            return new CheckResult(false, message, limit, spent, remaining);
        }

        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public BigDecimal getLimit() { return limit; }
        public BigDecimal getSpent() { return spent; }
        public BigDecimal getRemaining() { return remaining; }
    }

    public static class ExpenseRequest {
        private final String category;
        private final LocalDate date;
        private final BigDecimal amount;

        public ExpenseRequest(String category, LocalDate date, BigDecimal amount) {
            this.category = category;
            this.date = date;
            this.amount = amount;
        }

        public String getCategory() { return category; }
        public LocalDate getDate() { return date; }
        public BigDecimal getAmount() { return amount; }
    }

    public static class BudgetSummary {
        private final boolean hasBudget;
        private final String category;
        private final BigDecimal limit;
        private final BigDecimal spent;
        private final BigDecimal remaining;

        public BudgetSummary(boolean hasBudget, String category, BigDecimal limit, BigDecimal spent, BigDecimal remaining) {
            this.hasBudget = hasBudget;
            this.category = category;
            this.limit = limit != null ? limit : BigDecimal.ZERO;
            this.spent = spent != null ? spent : BigDecimal.ZERO;
            this.remaining = remaining != null ? remaining : BigDecimal.ZERO;
        }

        public boolean isHasBudget() { return hasBudget; }
        public String getCategory() { return category; }
        public BigDecimal getLimit() { return limit; }
        public BigDecimal getSpent() { return spent; }
        public BigDecimal getRemaining() { return remaining; }
    }

    public BudgetSummary getBudgetSummary(int userId, String category, LocalDate date) {
        if (category == null || date == null) {
            return new BudgetSummary(false, category, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        String canonicalCategory = normalizeCategory(category);
        String monthYear = YearMonth.from(date).toString();
        BigDecimal limit = findLimit(userId, canonicalCategory, monthYear);
        if (limit == null) {
            return new BudgetSummary(false, canonicalCategory, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal spent = getSpent(userId, canonicalCategory, monthYear);
        BigDecimal remaining = limit.subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        return new BudgetSummary(true, canonicalCategory, limit, spent, remaining);
    }

    private final IntelligenceDAO intelligenceDAO;

    public BudgetLimitService() {
        this(new IntelligenceDAO());
    }

    public BudgetLimitService(IntelligenceDAO intelligenceDAO) {
        this.intelligenceDAO = intelligenceDAO;
    }

    public CheckResult checkExpense(int userId, String category, LocalDate date, BigDecimal amount) {
        return checkExpenseExcluding(userId, category, date, amount, null, null, null);
    }

    public CheckResult checkExpenseExcluding(int userId, String category, LocalDate date,
                                             BigDecimal amount, String excludedCategory,
                                             LocalDate excludedDate, BigDecimal excludedAmount) {
        if (category == null || date == null || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CheckResult.allowed();
        }

        String canonicalCategory = normalizeCategory(category);
        if ("Healthcare".equalsIgnoreCase(canonicalCategory)) {
            return CheckResult.allowed();
        }

        String monthYear = YearMonth.from(date).toString();
        BigDecimal limit = findLimit(userId, canonicalCategory, monthYear);
        if (limit == null) {
            return CheckResult.allowed();
        }

        BigDecimal spent = getSpent(userId, canonicalCategory, monthYear);
        if (excludedCategory != null && excludedDate != null && excludedAmount != null
                && normalizeCategory(excludedCategory).equalsIgnoreCase(canonicalCategory)
                && YearMonth.from(excludedDate).toString().equals(monthYear)) {
            spent = spent.subtract(excludedAmount);
            if (spent.compareTo(BigDecimal.ZERO) < 0) {
                spent = BigDecimal.ZERO;
            }
        }

        BigDecimal remaining = limit.subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        if (spent.add(amount).compareTo(limit) > 0) {
            return CheckResult.blocked(limit, spent, remaining, canonicalCategory);
        }
        return CheckResult.allowed(limit, spent, remaining.subtract(amount));
    }

    public CheckResult checkBatch(int userId, List<ExpenseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return CheckResult.allowed();
        }

        Map<String, BatchKey> totals = new LinkedHashMap<>();
        for (ExpenseRequest request : requests) {
            if (request.category == null || request.date == null || request.amount == null) continue;
            String canonicalCategory = normalizeCategory(request.category);
            if ("Healthcare".equalsIgnoreCase(canonicalCategory)) continue;

            String monthYear = YearMonth.from(request.date).toString();
            String key = canonicalCategory.toLowerCase(Locale.ROOT) + "|" + monthYear;
            BatchKey batch = totals.get(key);
            if (batch == null) {
                batch = new BatchKey(canonicalCategory, monthYear);
                totals.put(key, batch);
            }
            batch.amount = batch.amount.add(request.amount);
        }

        for (BatchKey batch : totals.values()) {
            BigDecimal limit = findLimit(userId, batch.category, batch.monthYear);
            if (limit == null) continue;

            BigDecimal spent = getSpent(userId, batch.category, batch.monthYear);
            BigDecimal remaining = limit.subtract(spent);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            if (spent.add(batch.amount).compareTo(limit) > 0) {
                return CheckResult.blocked(limit, spent, remaining, batch.category);
            }
        }
        return CheckResult.allowed();
    }

    private BigDecimal findLimit(int userId, String category, String monthYear) {
        BigDecimal direct = intelligenceDAO.getBudgetLimitForMonth(userId, category, monthYear);
        if (direct != null) return direct;

        for (String alias : categoryAliases(category)) {
            BigDecimal aliasLimit = intelligenceDAO.getBudgetLimitForMonth(userId, alias, monthYear);
            if (aliasLimit != null) return aliasLimit;
        }
        return null;
    }

    private BigDecimal getSpent(int userId, String category, String monthYear) {
        BigDecimal spent = BigDecimal.ZERO;
        for (String alias : categoryAliases(category)) {
            spent = spent.add(intelligenceDAO.getMonthlyCategorySpend(userId, alias, monthYear));
        }
        return spent.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeCategory(String category) {
        String value = category == null ? "" : category.trim();
        if ("bills & utilities".equalsIgnoreCase(value) || "utility".equalsIgnoreCase(value)
                || "utilities".equalsIgnoreCase(value)) return "Utilities";
        if ("food".equalsIgnoreCase(value) || "groceries".equalsIgnoreCase(value)
                || "food & dining".equalsIgnoreCase(value)) return "Food & Dining";
        if ("rent".equalsIgnoreCase(value) || "rent & housing".equalsIgnoreCase(value)
                || "housing".equalsIgnoreCase(value)) return "Rent";
        if ("shopping".equalsIgnoreCase(value)) return "Shopping";
        if ("entertainment".equalsIgnoreCase(value)) return "Entertainment";
        if ("travel".equalsIgnoreCase(value) || "transport".equalsIgnoreCase(value)) return "Travel";
        if ("healthcare".equalsIgnoreCase(value) || "health".equalsIgnoreCase(value)
                || "medical".equalsIgnoreCase(value)) return "Healthcare";
        if ("other".equalsIgnoreCase(value)) return "Other";
        return value;
    }

    private static List<String> categoryAliases(String category) {
        List<String> aliases = new ArrayList<>();
        aliases.add(category);
        if ("Utilities".equals(category) || "Bills & Utilities".equals(category)) {
            aliases.add("Bills & Utilities");
            aliases.add("Utilities");
            aliases.add("Utility");
        } else if ("Food & Dining".equals(category) || "Food".equals(category)) {
            aliases.add("Food & Dining");
            aliases.add("Food");
            aliases.add("Groceries");
        } else if ("Rent".equals(category)) {
            aliases.add("Rent");
            aliases.add("Housing");
            aliases.add("Rent & Housing");
        } else if ("Travel".equals(category)) {
            aliases.add("Transport");
        } else if ("Healthcare".equals(category)) {
            aliases.add("Health");
            aliases.add("Medical");
        }
        return aliases;
    }

    private static String format(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static class BatchKey {
        private final String category;
        private final String monthYear;
        private BigDecimal amount = BigDecimal.ZERO;

        private BatchKey(String category, String monthYear) {
            this.category = category;
            this.monthYear = monthYear;
        }
    }
}
