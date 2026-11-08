package com.finsight.engine.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.finsight.engine.insights.FinancialInsight.InsightType;
import com.finsight.engine.insights.FinancialInsight.Severity;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Deterministic detection engine for unusual financial patterns.
 * 
 * Rules:
 * 1. Unusually large expense: An expense exceeding 2.5x the category median (or 3x category average).
 * 2. Category spending spike: Category net spending in current month exceeding 1.5x historical baseline.
 * 3. Potential duplicates: Identical/very similar date, amount, description/merchant. Never silently excludes them.
 * 4. Recurring payments: Weekly (6-8 days) or Monthly (27-33 days) intervals with consistent merchant & amount.
 */
public class UnusualTransactionDetector {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private final SpendingBaselineCalculator baselineCalculator;

    public UnusualTransactionDetector() {
        this(new SpendingBaselineCalculator());
    }

    public UnusualTransactionDetector(SpendingBaselineCalculator baselineCalculator) {
        this.baselineCalculator = Objects.requireNonNull(baselineCalculator, "baselineCalculator must not be null");
    }

    /**
     * Runs all deterministic detection rules and returns an explainable list of financial insights.
     */
    public List<FinancialInsight> detectAll(List<ParsedTransaction> transactions,
                                           LocalDate statementStart,
                                           LocalDate statementEnd) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<FinancialInsight> insights = new ArrayList<>();

        // 1. Potential duplicates
        insights.addAll(detectPotentialDuplicates(transactions, statementStart, statementEnd));

        // 2. Unusually large individual transactions
        insights.addAll(detectUnusuallyLargeExpenses(transactions, statementStart, statementEnd));

        // 3. Category spending spikes across months
        insights.addAll(detectCategorySpikes(transactions, statementStart, statementEnd));

        // 4. Recurring payments (weekly / monthly)
        insights.addAll(detectRecurringPayments(transactions, statementStart, statementEnd));

        return Collections.unmodifiableList(insights);
    }

    /**
     * A. Detects unusually large expenses relative to their category median.
     * Uses median expense for the category: flagged if transaction amount > max(median * 2.5, 1000.00).
     * Minimum 3 transactions in the category required for establishing a median.
     */
    public List<FinancialInsight> detectUnusuallyLargeExpenses(List<ParsedTransaction> transactions,
                                                              LocalDate statementStart,
                                                              LocalDate statementEnd) {
        List<FinancialInsight> list = new ArrayList<>();

        // Filter valid non-duplicate, non-transfer expenses within period
        List<ParsedTransaction> validExpenses = transactions.stream()
                .filter(tx -> !tx.isConfirmedDuplicate())
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .filter(tx -> isWithinPeriod(tx, statementStart, statementEnd))
                .collect(Collectors.toList());

        // Group by category
        Map<String, List<ParsedTransaction>> byCat = validExpenses.stream()
                .collect(Collectors.groupingBy(tx -> tx.getCategory() != null ? tx.getCategory() : "Other"));

        for (Map.Entry<String, List<ParsedTransaction>> entry : byCat.entrySet()) {
            String category = entry.getKey();
            List<ParsedTransaction> catTxs = entry.getValue();

            if (catTxs.size() < 3) {
                continue; // Insufficient category sample to establish normal distribution
            }

            List<BigDecimal> amounts = catTxs.stream()
                    .map(tx -> tx.getDebit() != null ? tx.getDebit() : tx.getAmount())
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            BigDecimal median = calculateMedian(amounts);
            if (median.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal minFloor = new BigDecimal("500.00");

            for (ParsedTransaction tx : catTxs) {
                BigDecimal txAmount = tx.getDebit() != null ? tx.getDebit() : tx.getAmount();
                if (txAmount == null) continue;

                // Evaluate reference median of OTHER transactions in category to prevent self-distortion
                List<BigDecimal> otherAmounts = catTxs.stream()
                        .filter(t -> !t.getId().equals(tx.getId()))
                        .map(t -> t.getDebit() != null ? t.getDebit() : t.getAmount())
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toList());

                // If other transactions are at least 2, use peer reference median; otherwise use category median
                BigDecimal refMedian = otherAmounts.size() >= 2 ? calculateMedian(otherAmounts) : median;
                if (refMedian.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal threshold = refMedian.multiply(new BigDecimal("2.50")).setScale(2, RM);
                BigDecimal effectiveThreshold = threshold.max(minFloor);

                if (txAmount.compareTo(effectiveThreshold) > 0) {
                    BigDecimal ratio = txAmount.divide(refMedian, 1, RM);
                    list.add(new FinancialInsight(
                            "INS-LARGE-" + tx.getId(),
                            InsightType.UNUSUAL_EXPENSE,
                            Severity.WARNING,
                            "Unusually Large Expense: " + category,
                            String.format("Transaction of %s is %sx higher than peer category median (%s).",
                                    txAmount.setScale(2, RM), ratio, refMedian.setScale(2, RM)),
                            category,
                            tx.getMerchant(),
                            txAmount,
                            Collections.singletonList(tx.getId()),
                            String.format("Amount: %s | Peer Category Median: %s | Multiplier: %sx | Effective Threshold: %s",
                                    txAmount, refMedian, ratio, effectiveThreshold),
                            "Verify if this was a one-off capital purchase, annual subscription, or an erroneous entry."
                    ));
                }
            }
        }

        return list;
    }

    /**
     * B. Detects category spending spikes comparing latest month spending against historical baseline.
     * Spike condition: Latest month spending > 1.50x baseline AND delta > 1000.00.
     */
    public List<FinancialInsight> detectCategorySpikes(List<ParsedTransaction> transactions,
                                                      LocalDate statementStart,
                                                      LocalDate statementEnd) {
        List<FinancialInsight> list = new ArrayList<>();

        Map<String, Map<YearMonth, BigDecimal>> monthlySpends = baselineCalculator.calculateMonthlyCategorySpending(
                transactions, statementStart, statementEnd);

        for (Map.Entry<String, Map<YearMonth, BigDecimal>> entry : monthlySpends.entrySet()) {
            String category = entry.getKey();
            Map<YearMonth, BigDecimal> months = entry.getValue();

            if (months.size() < 2) {
                continue; // Need at least 2 months to observe a spike
            }

            List<YearMonth> sorted = new ArrayList<>(months.keySet());
            Collections.sort(sorted);

            YearMonth latestYm = sorted.get(sorted.size() - 1);
            BigDecimal latestSpend = months.get(latestYm);

            // Baseline derived from preceding months
            List<BigDecimal> priorSpends = sorted.subList(0, sorted.size() - 1).stream()
                    .map(months::get)
                    .collect(Collectors.toList());

            BigDecimal baseline = priorSpends.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(priorSpends.size()), 2, RM);

            if (baseline.compareTo(new BigDecimal("500.00")) < 0) {
                continue; // Ignore very small categories to prevent noisy false positives
            }

            BigDecimal spikeThreshold = baseline.multiply(new BigDecimal("1.50")).setScale(2, RM);
            BigDecimal delta = latestSpend.subtract(baseline);

            if (latestSpend.compareTo(spikeThreshold) > 0 && delta.compareTo(new BigDecimal("1000.00")) >= 0) {
                BigDecimal ratio = latestSpend.divide(baseline, 2, RM);
                list.add(new FinancialInsight(
                        "INS-SPIKE-" + category.replaceAll("\\s+", "-") + "-" + latestYm,
                        InsightType.CATEGORY_SPENDING_SPIKE,
                        Severity.ALERT,
                        "Spending Spike in " + category,
                        String.format("Net spending for %s (%s) spiked by %s%% above historical baseline (%s).",
                                latestYm, latestSpend, ratio.subtract(BigDecimal.ONE).multiply(new BigDecimal("100")).setScale(0, RM), baseline),
                        category,
                        null,
                        latestSpend,
                        Collections.emptyList(),
                        String.format("Latest (%s): %s | Historical Baseline: %s | Ratio: %sx | Excess: %s",
                                latestYm, latestSpend, baseline, ratio, delta),
                        "Review recent charges in " + category + " to identify one-time large outlays."
                ));
            }
        }

        return list;
    }

    /**
     * C. Detects potential duplicate transactions with identical or near-identical attributes.
     * Criteria:
     * - Date difference <= 2 days
     * - Same amount
     * - Same transaction type
     * - Matching merchant or highly similar description
     * Suspected duplicates are preserved; never deleted or excluded from analytics here.
     */
    public List<FinancialInsight> detectPotentialDuplicates(List<ParsedTransaction> transactions,
                                                           LocalDate statementStart,
                                                           LocalDate statementEnd) {
        List<FinancialInsight> list = new ArrayList<>();
        Set<String> pairedIds = new HashSet<>();

        List<ParsedTransaction> validTxs = transactions.stream()
                .filter(tx -> isWithinPeriod(tx, statementStart, statementEnd))
                .collect(Collectors.toList());

        for (int i = 0; i < validTxs.size(); i++) {
            ParsedTransaction tx1 = validTxs.get(i);
            if (tx1.isConfirmedDuplicate()) continue;

            for (int j = i + 1; j < validTxs.size(); j++) {
                ParsedTransaction tx2 = validTxs.get(j);
                if (tx2.isConfirmedDuplicate()) continue;

                String pairKey = tx1.getId().compareTo(tx2.getId()) < 0 
                        ? tx1.getId() + ":" + tx2.getId() 
                        : tx2.getId() + ":" + tx1.getId();
                if (pairedIds.contains(pairKey)) continue;

                if (isPotentialDuplicatePair(tx1, tx2)) {
                    pairedIds.add(pairKey);
                    BigDecimal amt = tx1.getAmount();

                    List<String> ids = new ArrayList<>();
                    ids.add(tx1.getId());
                    ids.add(tx2.getId());

                    list.add(new FinancialInsight(
                            "INS-DUP-" + tx1.getId() + "-" + tx2.getId(),
                            InsightType.POTENTIAL_DUPLICATE,
                            Severity.WARNING,
                            "Potential Duplicate Transaction Detected",
                            String.format("Two transactions of %s on %s and %s share matching amounts and merchant details.",
                                    amt.setScale(2, RM), tx1.getTransactionDate(), tx2.getTransactionDate()),
                            tx1.getCategory(),
                            tx1.getMerchant(),
                            amt,
                            ids,
                            String.format("Tx 1: [%s, %s, %s] | Tx 2: [%s, %s, %s]",
                                    tx1.getId(), tx1.getTransactionDate(), tx1.getDescription(),
                                    tx2.getId(), tx2.getTransactionDate(), tx2.getDescription()),
                            "Review transactions with your bank to confirm whether a duplicate debit occurred or if this was a valid twin purchase."
                    ));
                }
            }
        }

        return list;
    }

    /**
     * D. Detects recurring payment patterns (weekly: 6-8 days, monthly: 27-33 days).
     * Requirements:
     * - Minimum 3 occurrences for monthly recurrence, minimum 4 for weekly recurrence.
     * - Matching merchant or description prefix.
     * - Consistent amounts (within 5% variance or exact).
     */
    public List<FinancialInsight> detectRecurringPayments(List<ParsedTransaction> transactions,
                                                         LocalDate statementStart,
                                                         LocalDate statementEnd) {
        List<FinancialInsight> list = new ArrayList<>();

        List<ParsedTransaction> expenses = transactions.stream()
                .filter(tx -> !tx.isConfirmedDuplicate())
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .filter(tx -> isWithinPeriod(tx, statementStart, statementEnd))
                .sorted(Comparator.comparing(ParsedTransaction::getTransactionDate))
                .collect(Collectors.toList());

        // Group by entity key: merchant name (if known) or clean description token
        Map<String, List<ParsedTransaction>> byEntity = new HashMap<>();
        for (ParsedTransaction tx : expenses) {
            String key = getRecurringEntityKey(tx);
            if (key != null && !key.isEmpty()) {
                byEntity.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
            }
        }

        for (Map.Entry<String, List<ParsedTransaction>> entry : byEntity.entrySet()) {
            String entity = entry.getKey();
            List<ParsedTransaction> txs = entry.getValue();

            if (txs.size() < 3) continue; // Minimum 3 occurrences required

            // Check Monthly pattern (intervals between 27 and 33 days)
            if (matchesRecurrenceInterval(txs, 27, 33, 3)) {
                BigDecimal sampleAmount = txs.get(0).getAmount();
                List<String> ids = txs.stream().map(ParsedTransaction::getId).collect(Collectors.toList());
                list.add(new FinancialInsight(
                        "INS-REC-MONTHLY-" + entity.replaceAll("\\s+", "-"),
                        InsightType.RECURRING_PAYMENT,
                        Severity.INFO,
                        "Recurring Monthly Payment: " + entity,
                        String.format("Identified %d regular monthly payments of ~%s for %s.",
                                txs.size(), sampleAmount.setScale(2, RM), entity),
                        txs.get(0).getCategory(),
                        txs.get(0).getMerchant(),
                        sampleAmount,
                        ids,
                        String.format("Occurrences: %d | Interval: ~30 days | Average Amount: %s",
                                txs.size(), sampleAmount.setScale(2, RM)),
                        "Consider this recurring subscription when planning your monthly budget."
                ));
            } else if (txs.size() >= 4 && matchesRecurrenceInterval(txs, 6, 8, 4)) {
                // Check Weekly pattern (intervals between 6 and 8 days)
                BigDecimal sampleAmount = txs.get(0).getAmount();
                List<String> ids = txs.stream().map(ParsedTransaction::getId).collect(Collectors.toList());
                list.add(new FinancialInsight(
                        "INS-REC-WEEKLY-" + entity.replaceAll("\\s+", "-"),
                        InsightType.RECURRING_PAYMENT,
                        Severity.INFO,
                        "Recurring Weekly Payment: " + entity,
                        String.format("Identified %d regular weekly payments of ~%s for %s.",
                                txs.size(), sampleAmount.setScale(2, RM), entity),
                        txs.get(0).getCategory(),
                        txs.get(0).getMerchant(),
                        sampleAmount,
                        ids,
                        String.format("Occurrences: %d | Interval: ~7 days | Average Amount: %s",
                                txs.size(), sampleAmount.setScale(2, RM)),
                        "Regular weekly expense. Account for this in your weekly cash flow planning."
                ));
            }
        }

        return list;
    }

    private boolean isWithinPeriod(ParsedTransaction tx, LocalDate start, LocalDate end) {
        if (tx == null) return false;
        LocalDate d = tx.getTransactionDate();
        if (d == null) return true;
        if (start != null && d.isBefore(start)) return false;
        if (end != null && d.isAfter(end)) return false;
        return true;
    }

    private boolean isPotentialDuplicatePair(ParsedTransaction t1, ParsedTransaction t2) {
        if (t1.getTransactionType() != t2.getTransactionType()) return false;
        if (t1.getTransactionDate() == null || t2.getTransactionDate() == null) return false;

        long daysDiff = Math.abs(ChronoUnit.DAYS.between(t1.getTransactionDate(), t2.getTransactionDate()));
        if (daysDiff > 2) return false; // Must be within 2 days

        BigDecimal a1 = t1.getAmount();
        BigDecimal a2 = t2.getAmount();
        if (a1 == null || a2 == null || a1.compareTo(a2) != 0) return false; // Exact amount match

        // Merchant or description matching
        if (t1.getMerchant() != null && t2.getMerchant() != null
                && !t1.getMerchant().equalsIgnoreCase("Unknown")
                && t1.getMerchant().equalsIgnoreCase(t2.getMerchant())) {
            return true;
        }

        String d1 = t1.getDescription() != null ? t1.getDescription().trim().toLowerCase() : "";
        String d2 = t2.getDescription() != null ? t2.getDescription().trim().toLowerCase() : "";
        if (!d1.isEmpty() && d1.equals(d2)) {
            return true;
        }

        return false;
    }

    private String getRecurringEntityKey(ParsedTransaction tx) {
        if (tx.getMerchant() != null && !tx.getMerchant().equalsIgnoreCase("Unknown") && !tx.getMerchant().trim().isEmpty()) {
            return tx.getMerchant().trim();
        }
        if (tx.getDescription() != null) {
            String desc = tx.getDescription().trim();
            int cut = desc.indexOf(' ');
            return cut > 3 ? desc.substring(0, cut).trim() : desc;
        }
        return null;
    }

    private boolean matchesRecurrenceInterval(List<ParsedTransaction> txs, int minDays, int maxDays, int minCount) {
        int consecutiveMatches = 0;
        BigDecimal baseAmt = txs.get(0).getAmount();

        for (int i = 1; i < txs.size(); i++) {
            ParsedTransaction prev = txs.get(i - 1);
            ParsedTransaction curr = txs.get(i);

            long days = ChronoUnit.DAYS.between(prev.getTransactionDate(), curr.getTransactionDate());
            boolean daysMatch = (days >= minDays && days <= maxDays);

            // Amount consistency (within 5% variance)
            BigDecimal currAmt = curr.getAmount();
            boolean amtMatch = false;
            if (baseAmt != null && currAmt != null && baseAmt.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = currAmt.subtract(baseAmt).abs();
                BigDecimal ratio = diff.divide(baseAmt, 4, RM);
                amtMatch = ratio.compareTo(new BigDecimal("0.05")) <= 0;
            }

            if (daysMatch && amtMatch) {
                consecutiveMatches++;
            }
        }

        return consecutiveMatches >= (minCount - 1);
    }

    private BigDecimal calculateMedian(List<BigDecimal> sorted) {
        if (sorted == null || sorted.isEmpty()) return BigDecimal.ZERO;
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        } else {
            return sorted.get((n / 2) - 1).add(sorted.get(n / 2))
                    .divide(new BigDecimal("2"), 2, RM);
        }
    }
}

