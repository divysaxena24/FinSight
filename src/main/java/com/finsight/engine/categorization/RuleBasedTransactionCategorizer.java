package com.finsight.engine.categorization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Deterministic, rule-based transaction categorizer and normalizer.
 * 
 * Rules & Invariants:
 * 1. Supports 13 canonical categories:
 *    Food & Dining, Shopping, Transport, Housing & Rent, Utilities, Healthcare,
 *    Entertainment, Education, Salary, Investment, EMI & Loans, Transfers, Other.
 * 2. High-priority explicit rules are evaluated before broad generic patterns.
 * 3. Directional Context: Credits for 'Salary', 'Dividend', 'Refund' differ from debits.
 * 4. Never classifies transactions based on amount alone.
 * 5. Does not classify every UPI payment as a transfer (distinguishes merchant UPI from P2P/self transfer).
 * 6. Explicit Confidence & Review Policy:
 *    - Confidence >= 0.85: Matched category, needsUserReview = false.
 *    - Confidence >= 0.70 and < 0.85: Matched category, needsUserReview = true.
 *    - Confidence < 0.70: Category Other, needsUserReview = true.
 * 7. Non-Destructive Refund Policy:
 *    - Refunds do not blindly default to "Shopping".
 *    - If an original purchase category is established via merchant or existing context, it is preserved.
 *    - If the original category is unknown, assigns safe fallback "Other", sets isRefundOrReversal = true,
 *      and marks needsUserReview = true.
 * 8. Manual Correction Safety: If transaction.isManuallyCategorized() == true,
 *    categorization logic preserves the user's category without overwriting.
 */
public class RuleBasedTransactionCategorizer implements TransactionCategorizer {

    public static final BigDecimal CONFIDENCE_HIGH_THRESHOLD = new BigDecimal("0.85");
    public static final BigDecimal CONFIDENCE_MEDIUM_THRESHOLD = new BigDecimal("0.70");

    public static final BigDecimal CONFIDENCE_HIGH = new BigDecimal("0.90");
    public static final BigDecimal CONFIDENCE_MEDIUM = new BigDecimal("0.75");
    public static final BigDecimal CONFIDENCE_LOW = new BigDecimal("0.50");

    private final MerchantNormalizer merchantNormalizer;
    private final List<CategoryRule> rules;

    public RuleBasedTransactionCategorizer() {
        this.merchantNormalizer = new MerchantNormalizer();
        this.rules = initializeRules();
    }

    public RuleBasedTransactionCategorizer(MerchantNormalizer normalizer, List<CategoryRule> customRules) {
        this.merchantNormalizer = normalizer != null ? normalizer : new MerchantNormalizer();
        this.rules = customRules != null ? new ArrayList<>(customRules) : initializeRules();
        this.rules.sort(Comparator.comparingInt(CategoryRule::getPriority).reversed());
    }

    private static List<CategoryRule> initializeRules() {
        List<CategoryRule> r = new ArrayList<>();

        // Priority 100: Explicit Salary & Payroll credits
        r.add(new CategoryRule("SAL_CREDIT", "Salary", "\\b(?:SALARY|PAYROLL|STIPEND|MONTHLY SALARY)\\b", TransactionType.INCOME, false, false, new BigDecimal("0.95"), 100));

        // Priority 90: Explicit Own-Account Transfers / Self NEFT / Sweep
        r.add(new CategoryRule("TRANSFER_SELF", "Transfers", "\\b(?:OWN A/C|SELF TRANSFER|SWEEP TRF|TO SAVINGS|TO CURRENT|TRF TO SELF)\\b", null, true, false, new BigDecimal("0.95"), 90));
        r.add(new CategoryRule("TRANSFER_P2P", "Transfers", "\\b(?:NEFT|RTGS|IMPS)\\b.*\\b(?:TRANSFER TO|SENT TO)\\b", null, true, false, new BigDecimal("0.85"), 90));

        // Priority 85: Food & Dining Merchants & Keywords
        r.add(new CategoryRule("FOOD_KNOWN_MERCH", "Food & Dining", "\\b(?:SWIGGY|ZOMATO|STARBUCKS|MCDONALD|DOMINO|KFC|BURGER KING|BLINKIT|ZEPTO|BIGBASKET)\\b", null, false, false, new BigDecimal("0.95"), 85));
        r.add(new CategoryRule("FOOD_KEYWORDS", "Food & Dining", "\\b(?:RESTAURANT|CAFE|BAKERY|COFFEE|DINER|PIZZA|BURGER|BAR & RESTAURANT|BISTRO|SWEETS)\\b", null, false, false, new BigDecimal("0.80"), 85));

        // Priority 80: Shopping Merchants & Retail
        r.add(new CategoryRule("SHOP_KNOWN_MERCH", "Shopping", "\\b(?:AMAZON|AMZN|FLIPKART|MYNTRA|AJIO|NYKAA|TATA CLIQ|CROMA|RELIANCE DIGITAL|IKEA|DECATHLON|ZARA|H&M)\\b", null, false, false, new BigDecimal("0.95"), 80));
        r.add(new CategoryRule("SHOP_KEYWORDS", "Shopping", "\\b(?:RETAIL|STORE|SUPERMARKET|MART|HYPERMARKET|MALL|CLOTHING|APPAREL|FOOTWEAR|JEWELLERS|FASHION)\\b", null, false, false, new BigDecimal("0.75"), 80));

        // Priority 75: Transport & Travel
        r.add(new CategoryRule("TRANS_KNOWN_MERCH", "Transport", "\\b(?:UBER|OLA|RAPIDO|IRCTC|INDIGO|AIR INDIA|MAKEMYTRIP|REDBUS|FASTAG)\\b", null, false, false, new BigDecimal("0.95"), 75));
        r.add(new CategoryRule("TRANS_KEYWORDS", "Transport", "\\b(?:METRO|RAILWAYS|BUS|TAXI|CAB|AIRLINES|FLIGHT|FUEL|PETROL|DIESEL|INDIAN OIL|BHARAT PETROL|SHELL|TOLL)\\b", null, false, false, new BigDecimal("0.80"), 75));

        // Priority 70: Utilities & Telecom
        r.add(new CategoryRule("UTIL_KNOWN_MERCH", "Utilities", "\\b(?:AIRTEL|JIO|VODAFONE|BESCOM|ADANI ELECTRICITY|MAHAVITARAN|TATA POWER|ACT FIBERNET)\\b", null, false, false, new BigDecimal("0.95"), 70));
        r.add(new CategoryRule("UTIL_KEYWORDS", "Utilities", "\\b(?:ELECTRICITY|WATER BILL|GAS BILL|BROADBAND|POSTPAID|DTH|POWER CORP|UTILITY BILL)\\b", null, false, false, new BigDecimal("0.80"), 70));

        // Priority 65: Housing & Rent
        r.add(new CategoryRule("RENT_KEYWORDS", "Housing & Rent", "\\b(?:HOUSE RENT|APARTMENT RENT|RENT PAYMENT|SOCIETY MAINTENANCE|LANDLORD|HOUSING)\\b", null, false, false, new BigDecimal("0.90"), 65));

        // Priority 60: Healthcare & Medical
        r.add(new CategoryRule("HEALTH_KNOWN_MERCH", "Healthcare", "\\b(?:APOLLO|1MG|PHARMEASY|MEDPLUS|FORTIS)\\b", null, false, false, new BigDecimal("0.95"), 60));
        r.add(new CategoryRule("HEALTH_KEYWORDS", "Healthcare", "\\b(?:PHARMACY|HOSPITAL|CLINIC|DIAGNOSTICS|LABS|MEDICAL|HEALTHCARE|DENTAL|OPTICAL)\\b", null, false, false, new BigDecimal("0.80"), 60));

        // Priority 55: Entertainment & Subscriptions
        r.add(new CategoryRule("ENT_KNOWN_MERCH", "Entertainment", "\\b(?:NETFLIX|SPOTIFY|HOTSTAR|BOOKMYSHOW|PRIME VIDEO|SONY LIV|YOUTUBE|PVR|INOX)\\b", null, false, false, new BigDecimal("0.95"), 55));
        r.add(new CategoryRule("ENT_KEYWORDS", "Entertainment", "\\b(?:CINEMA|THEATRE|MOVIES|GAMING|CONCERT|ENTERTAINMENT|SUBSCRIPTION)\\b", null, false, false, new BigDecimal("0.75"), 55));

        // Priority 50: Education & Learning
        r.add(new CategoryRule("EDU_KEYWORDS", "Education", "\\b(?:COLLEGE|UNIVERSITY|SCHOOL FEE|TUITION|UDEMY|COURSERA|EDX|BOOKS|INSTITUTE|EXAM FEE)\\b", null, false, false, new BigDecimal("0.85"), 50));

        // Priority 45: Investment & Wealth
        r.add(new CategoryRule("INV_KNOWN_MERCH", "Investment", "\\b(?:ZERODHA|GROWW|UPSTOX|INDMONEY)\\b", null, false, false, new BigDecimal("0.95"), 45));
        r.add(new CategoryRule("INV_KEYWORDS", "Investment", "\\b(?:MUTUAL FUND|SIP|STOCKS|SECURITIES|NSE|BSE|DEMAT|DIVIDEND|INTEREST EARNED)\\b", null, false, false, new BigDecimal("0.85"), 45));

        // Priority 40: EMI & Loans
        r.add(new CategoryRule("LOAN_KEYWORDS", "EMI & Loans", "\\b(?:LOAN EMI|AUTO LOAN|HOME LOAN|PERSONAL LOAN|BAJAJ FIN|EMI DEBIT|FINANCE EMI)\\b", null, false, false, new BigDecimal("0.90"), 40));

        // Priority 30: Generic P2P UPI (Non-merchant, low confidence transfer candidate)
        r.add(new CategoryRule("UPI_P2P_GENERIC", "Transfers", "\\bUPI/[0-9]+/.*", null, true, false, new BigDecimal("0.70"), 30));

        // Sort descending by priority
        r.sort(Comparator.comparingInt(CategoryRule::getPriority).reversed());
        return Collections.unmodifiableList(r);
    }

    @Override
    public CategorizationResult categorize(ParsedTransaction transaction) {
        if (transaction == null) {
            return CategorizationResult.unclassified("Unknown", Collections.singletonList("Null transaction provided."));
        }

        List<String> auditTrail = new ArrayList<>();
        String rawDesc = transaction.getDescription();
        String merchant = merchantNormalizer.normalize(rawDesc);
        auditTrail.add("Normalized merchant: " + merchant);

        // Check if narration explicitly denotes a refund, reversal, or return
        boolean hasRefundToken = containsRefundToken(rawDesc);
        boolean isRefund = transaction.isRefundOrReversal() || (hasRefundToken && transaction.getTransactionType() == TransactionType.INCOME);

        // 1. If manually categorized, preserve existing category with maximum confidence
        if (transaction.isManuallyCategorized()) {
            auditTrail.add("Transaction is marked as manually categorized: preserving '" + transaction.getCategory() + "'");
            return new CategorizationResult(
                    transaction.getCategory(),
                    merchant,
                    BigDecimal.ONE,
                    false,
                    transaction.getTransactionType() == TransactionType.TRANSFER,
                    isRefund,
                    "MANUAL_OVERRIDE",
                    auditTrail
            );
        }

        // 2. If already categorized (e.g. from upstream bank parser or purchase linkage) and this is a refund, preserve it!
        if (isRefund && transaction.getCategory() != null && !transaction.getCategory().trim().isEmpty() 
                && !"Other".equalsIgnoreCase(transaction.getCategory()) && !"Uncategorized".equalsIgnoreCase(transaction.getCategory())) {
            auditTrail.add("Preserving existing category for refund: " + transaction.getCategory());
            return new CategorizationResult(
                    transaction.getCategory(),
                    merchant,
                    CONFIDENCE_HIGH,
                    false,
                    false,
                    true,
                    "PRESERVE_REFUND_CATEGORY",
                    auditTrail
            );
        }

        String searchContext = (rawDesc != null ? rawDesc : "") + " " + (merchant != null ? merchant : "");
        TransactionType type = transaction.getTransactionType();

        // 3. Evaluate domain rules against the transaction
        for (CategoryRule rule : rules) {
            if (rule.matches(searchContext, type)) {
                auditTrail.add("Matched rule: " + rule.getRuleId() + " -> " + rule.getTargetCategory() 
                        + " (confidence: " + rule.getConfidence() + ")");

                BigDecimal conf = rule.getConfidence();
                String assignedCategory;
                boolean needsReview;

                // Policy:
                // >= 0.85: matched category, needsUserReview = false
                // >= 0.70 and < 0.85: matched category, needsUserReview = true
                // < 0.70: category Other, needsUserReview = true
                if (conf.compareTo(CONFIDENCE_HIGH_THRESHOLD) >= 0) {
                    assignedCategory = rule.getTargetCategory();
                    needsReview = false;
                } else if (conf.compareTo(CONFIDENCE_MEDIUM_THRESHOLD) >= 0) {
                    assignedCategory = rule.getTargetCategory();
                    needsReview = true;
                } else {
                    assignedCategory = "Other";
                    needsReview = true;
                }

                return new CategorizationResult(
                        assignedCategory,
                        merchant,
                        conf,
                        needsReview,
                        rule.isTransfer(),
                        isRefund || rule.isRefund(),
                        rule.getRuleId(),
                        auditTrail
                );
            }
        }

        // 4. Fallback for unclassified transactions or unlinked generic refunds
        if (isRefund) {
            auditTrail.add("Refund with no identifiable merchant/purchase category. Assigned fallback 'Other' for user review.");
            return new CategorizationResult(
                    "Other",
                    merchant,
                    CONFIDENCE_LOW,
                    true,
                    false,
                    true,
                    "REFUND_UNLINKED_FALLBACK",
                    auditTrail
            );
        }

        auditTrail.add("No specific rule matched. Assigned fallback 'Other'.");
        return new CategorizationResult(
                "Other",
                merchant,
                CONFIDENCE_LOW,
                true,
                false,
                false,
                "FALLBACK_UNMATCHED",
                auditTrail
        );
    }

    @Override
    public void applyCategorization(ParsedTransaction transaction) {
        if (transaction == null) {
            return;
        }

        if (transaction.isManuallyCategorized()) {
            return;
        }

        CategorizationResult result = categorize(transaction);

        transaction.setCategory(result.getCategory());
        transaction.setMerchant(result.getNormalizedMerchant());
        transaction.setConfidenceScore(result.getConfidence());
        transaction.setNeedsUserReview(result.isNeedsUserReview());

        if (result.isTransfer()) {
            transaction.setTransactionType(TransactionType.TRANSFER);
        }
        if (result.isRefundOrReversal()) {
            transaction.setRefundOrReversal(true);
        }
    }

    public static boolean containsRefundToken(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String upper = text.toUpperCase();
        return upper.contains("REFUND") 
                || upper.contains("REVERSAL") 
                || upper.contains("REV-") 
                || upper.contains("RETURNED") 
                || upper.contains("CASHBACK");
    }

    public MerchantNormalizer getMerchantNormalizer() {
        return merchantNormalizer;
    }

    public List<CategoryRule> getRules() {
        return rules;
    }
}
