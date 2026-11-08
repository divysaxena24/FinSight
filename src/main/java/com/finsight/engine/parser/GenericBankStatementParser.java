package com.finsight.engine.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.model.ParsedTransaction.TransactionType;

/**
 * Generic Bank Statement Parser.
 *
 * Recognizes and parses standard digital bank statements based on tabular
 * and positional structural patterns without relying on hardcoded bank-specific identities.
 *
 * Supported features:
 * 1. Tabular format with dynamic column headers (Date, Description/Narration, Debit, Credit, Balance, etc.).
 * 2. Positional format (e.g. DATE CHANNEL MERCHANT CATEGORY DEBIT_OR_CREDIT AMOUNT NARRATION BALANCE).
 * 3. Multi-format date parsing (YYYY-MM-DD, DD/MM/YYYY, DD-MM-YYYY, DD MMM YYYY).
 * 4. Indian & Western currency numbering conventions with thousands separators.
 * 5. Safe multi-line narration continuation.
 * 6. Mathematical reconciliation across Opening Balance, Transactions, and Closing/Running Balances.
 * 7. Conservative structural detection confidence that firmly rejects arbitrary unstructured text
 *    and scales dynamically with actual parsed transaction rows.
 */
public class GenericBankStatementParser implements BankStatementParser {

    public static final String FORMAT_ID = "GENERIC_TABULAR_V1";
    public static final String BANK_NAME = "Generic Bank Statement";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
    );

    // Header token regexes
    private static final Pattern DATE_HEADER_PATTERN = Pattern.compile("(?i)^(txn\\s*date|transaction\\s*date|value\\s*date|post\\s*date|date)$");
    private static final Pattern DESC_HEADER_PATTERN = Pattern.compile("(?i)^(narration|particulars|transaction\\s*details|description|details|remarks)$");
    private static final Pattern DEBIT_HEADER_PATTERN = Pattern.compile("(?i)^(debit(\\s*amount)?|withdrawal(\\s*amount)?|withdrawals|dr|debit\\s*\\(dr\\))$");
    private static final Pattern CREDIT_HEADER_PATTERN = Pattern.compile("(?i)^(credit(\\s*amount)?|deposit(\\s*amount)?|deposits|cr|credit\\s*\\(cr\\))$");
    private static final Pattern BALANCE_HEADER_PATTERN = Pattern.compile("(?i)^(balance|closing\\s*balance|available\\s*balance|bal)$");
    private static final Pattern REF_HEADER_PATTERN = Pattern.compile("(?i)^(chq(\\.|\\/)?ref(\\.|\\s*no)?|reference(\\s*number|\\s*no)?|ref(\\s*no)?|utr(\\s*no)?|cheque(\\s*number|\\s*no)?|transaction\\s*id|txn\\s*id)$");
    private static final Pattern AMOUNT_HEADER_PATTERN = Pattern.compile("(?i)^(amount|txn\\s*amount|transaction\\s*amount)$");
    private static final Pattern TYPE_HEADER_PATTERN = Pattern.compile("(?i)^(type|cr\\/dr|dr\\/cr|txn\\s*type|debit\\/credit|credit\\/debit)$");
    private static final Pattern CHANNEL_HEADER_PATTERN = Pattern.compile("(?i)^(channel|mode|payment\\s*mode)$");
    private static final Pattern MERCHANT_HEADER_PATTERN = Pattern.compile("(?i)^(merchant|payee|beneficiary)$");
    private static final Pattern CATEGORY_HEADER_PATTERN = Pattern.compile("(?i)^(category)$");

    // Amount extraction pattern: supports Indian formatting (e.g. 1,50,000.00), standard commas (150,000.00) or plain
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[-+]?(?:Rs\\.?|INR|₹)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,4})?|[0-9]+(?:\\.[0-9]{1,4})?)$", Pattern.CASE_INSENSITIVE);

    // Opening / Closing balance indicators in text
    private static final Pattern OPENING_BALANCE_PATTERN = Pattern.compile("(?i)(?:opening\\s*balance|b\\/f|brought\\s*forward|opening\\s*bal)(?:\\s*[:\\-]?\\s*[-+]?(?:Rs\\.?|INR|₹)?\\s*)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,4})?|[0-9]+(?:\\.[0-9]{1,4})?)");
    private static final Pattern CLOSING_BALANCE_PATTERN = Pattern.compile("(?i)(?:closing\\s*balance|c\\/f|carried\\s*forward|closing\\s*bal)(?:\\s*[:\\-]?\\s*[-+]?(?:Rs\\.?|INR|₹)?\\s*)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,4})?|[0-9]+(?:\\.[0-9]{1,4})?)");

    // Known payment channels for positional matching
    private static final List<String> KNOWN_CHANNELS = Arrays.asList("UPI", "CARD", "IMPS", "BBPS", "NEFT", "RTGS", "ATM", "POS", "NETBANKING", "CHQ", "CHEQUE");

    public enum ColumnType {
        DATE,
        DESCRIPTION,
        DEBIT,
        CREDIT,
        BALANCE,
        REFERENCE,
        AMOUNT,
        TYPE,
        CHANNEL,
        MERCHANT,
        CATEGORY,
        UNKNOWN
    }

    public static class ColumnDefinition {
        final ColumnType type;
        final int index;
        final String rawHeader;

        ColumnDefinition(ColumnType type, int index, String rawHeader) {
            this.type = type;
            this.index = index;
            this.rawHeader = rawHeader;
        }
    }

    public static class TableSchema {
        final int headerLineIndex;
        final List<ColumnDefinition> columns;
        final boolean hasSeparateDebitCredit;
        final boolean hasAmountAndType;
        final boolean hasBalance;
        final boolean hasReference;
        final boolean isPositionalWithHeader;

        TableSchema(int headerLineIndex, List<ColumnDefinition> columns) {
            this.headerLineIndex = headerLineIndex;
            this.columns = columns;

            boolean debit = false;
            boolean credit = false;
            boolean amount = false;
            boolean type = false;
            boolean bal = false;
            boolean ref = false;
            boolean chan = false;
            boolean merch = false;

            for (ColumnDefinition col : columns) {
                if (col.type == ColumnType.DEBIT) debit = true;
                if (col.type == ColumnType.CREDIT) credit = true;
                if (col.type == ColumnType.AMOUNT) amount = true;
                if (col.type == ColumnType.TYPE) type = true;
                if (col.type == ColumnType.BALANCE) bal = true;
                if (col.type == ColumnType.REFERENCE) ref = true;
                if (col.type == ColumnType.CHANNEL) chan = true;
                if (col.type == ColumnType.MERCHANT) merch = true;
            }

            this.hasSeparateDebitCredit = debit && credit;
            this.hasAmountAndType = amount && type;
            this.hasBalance = bal;
            this.hasReference = ref;
            this.isPositionalWithHeader = chan || merch;
        }

        public ColumnDefinition getColumn(ColumnType type) {
            for (ColumnDefinition col : columns) {
                if (col.type == type) {
                    return col;
                }
            }
            return null;
        }

        public boolean isValid() {
            boolean hasDate = getColumn(ColumnType.DATE) != null;
            boolean hasDesc = getColumn(ColumnType.DESCRIPTION) != null || isPositionalWithHeader;
            boolean validMonetary = hasSeparateDebitCredit || hasAmountAndType;
            return hasDate && hasDesc && validMonetary;
        }
    }

    @Override
    public String getFormatId() {
        return FORMAT_ID;
    }

    @Override
    public String getBankName() {
        return BANK_NAME;
    }

    @Override
    public BigDecimal detectConfidence(String statementText) {
        if (statementText == null || statementText.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String[] lines = statementText.split("\\r?\\n");
        TableSchema schema = findTableSchema(lines);

        int validPositionalRows = 0;
        int validTabularRows = 0;
        int candidateRows = 0;

        int startIndex = (schema != null && schema.isValid()) ? schema.headerLineIndex + 1 : 0;

        for (int i = startIndex; i < lines.length && candidateRows < 50; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || isHeaderOrFooterLine(line)) {
                continue;
            }
            candidateRows++;
            if (schema != null && schema.isValid() && isPotentialTabularRow(line, schema)) {
                validTabularRows++;
            }
            if (parsePositionalRow(line, candidateRows) != null) {
                validPositionalRows++;
            }
        }

        // Rule: If zero valid transactions are parsed, confidence MUST be 0.00
        int validRows = Math.max(validTabularRows, validPositionalRows);
        if (validRows == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Structural base confidence:
        // Header-backed tabular or verified positional schema
        BigDecimal base = (schema != null && schema.isValid()) ? new BigDecimal("0.75") : new BigDecimal("0.70");

        // Scale by parsing ratio and count
        double parseRatio = (candidateRows > 0) ? (double) validRows / candidateRows : 0.0;
        if (parseRatio >= 0.80) {
            base = base.add(new BigDecimal("0.10"));
        } else if (parseRatio >= 0.50) {
            base = base.add(new BigDecimal("0.05"));
        }

        if (validRows >= 5) {
            base = base.add(new BigDecimal("0.05"));
        }

        return base.min(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public StatementParseResult parse(String statementText) {
        if (statementText == null || statementText.trim().isEmpty()) {
            return StatementParseResult.failure(
                    StatementParseResult.Status.EMPTY_INPUT,
                    FORMAT_ID,
                    BigDecimal.ZERO,
                    "Statement text is null or blank.",
                    Collections.emptyList()
            );
        }

        String[] lines = statementText.split("\\r?\\n");
        TableSchema schema = findTableSchema(lines);

        // Check if there are any positional rows present in the text
        boolean hasPositionalRows = false;
        for (String l : lines) {
            if (parsePositionalRow(l.trim(), 1) != null) {
                hasPositionalRows = true;
                break;
            }
        }

        if ((schema == null || !schema.isValid()) && !hasPositionalRows) {
            return StatementParseResult.failure(
                    StatementParseResult.Status.UNSUPPORTED_FORMAT,
                    FORMAT_ID,
                    BigDecimal.ZERO,
                    "No recognizable tabular bank statement headers or positional transaction structures found.",
                    Collections.emptyList()
            );
        }

        List<ParsedTransaction> parsedTransactions = new ArrayList<>();
        List<String> unparsedLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int headerIndex = (schema != null && schema.isValid()) ? schema.headerLineIndex : -1;
        BigDecimal extractedOpeningBalance = extractOpeningBalance(lines, headerIndex >= 0 ? headerIndex : lines.length);
        BigDecimal extractedClosingBalance = extractClosingBalance(lines);

        ParsedTransaction currentTx = null;

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // Skip header line itself
            if (schema != null && schema.isValid() && i == schema.headerLineIndex) {
                continue;
            }

            // Skip title/header lines preceding table schema
            if (schema != null && schema.isValid() && i < schema.headerLineIndex) {
                continue;
            }

            // Check if this line is a repeated page header
            TableSchema repeatedHeader = detectSchemaOnLine(trimmed, i);
            if (repeatedHeader != null && repeatedHeader.isValid()) {
                continue;
            }

            if (isHeaderOrFooterLine(trimmed)) {
                continue;
            }

            // 1. Try parsing as Positional row: DATE CHANNEL MERCHANT CATEGORY DEBIT_OR_CREDIT AMOUNT NARRATION BALANCE
            ParsedTransaction rowTx = parsePositionalRow(trimmed, parsedTransactions.size() + 1);

            // 2. If positional did not match, try tabular schema if available
            if (rowTx == null && schema != null && schema.isValid() && i > schema.headerLineIndex) {
                rowTx = tryParseTabularRow(trimmed, schema, parsedTransactions.size() + 1);
            }

            if (rowTx != null) {
                if (currentTx != null) {
                    parsedTransactions.add(currentTx);
                }
                currentTx = rowTx;
            } else {
                // Not a standalone transaction row. Could it be narration continuation?
                // Only consider continuation if the line does NOT look like an independent rejected or malformed row
                if (currentTx != null && isContinuationLine(trimmed) && !isIndependentRejectedRow(trimmed)) {
                    String updatedDesc = currentTx.getDescription() + " " + trimmed;
                    currentTx = new ParsedTransaction(
                            currentTx.getId(),
                            currentTx.getTransactionDate(),
                            updatedDesc,
                            currentTx.getDebit(),
                            currentTx.getCredit(),
                            currentTx.getAmount(),
                            currentTx.getTransactionType(),
                            currentTx.getBalance(),
                            currentTx.getMerchant(),
                            currentTx.getCategory(),
                            currentTx.getConfidenceScore(),
                            currentTx.getRawLine() + "\n" + rawLine,
                            currentTx.getBankFormat(),
                            currentTx.isNeedsUserReview(),
                            currentTx.isDuplicateCandidate(),
                            currentTx.isConfirmedDuplicate(),
                            currentTx.isRefundOrReversal(),
                            currentTx.isManuallyCategorized()
                    );
                } else {
                    unparsedLines.add(rawLine);
                }
            }
        }

        if (currentTx != null) {
            parsedTransactions.add(currentTx);
        }

        if (parsedTransactions.isEmpty()) {
            return StatementParseResult.failure(
                    StatementParseResult.Status.PARSING_FAILURE,
                    FORMAT_ID,
                    BigDecimal.ZERO,
                    "Statement format recognized but zero valid transactions could be parsed.",
                    warnings
            );
        }

        // Calculate dynamic confidence based on actual parsed results
        int totalRows = parsedTransactions.size() + unparsedLines.size();
        BigDecimal confidence = computeParseConfidence(parsedTransactions.size(), totalRows, schema != null && schema.isValid());

        // Perform financial reconciliation
        performReconciliation(parsedTransactions, extractedOpeningBalance, extractedClosingBalance, warnings);

        return StatementParseResult.success(
                FORMAT_ID,
                confidence,
                parsedTransactions,
                unparsedLines,
                warnings
        );
    }

    private BigDecimal computeParseConfidence(int parsedCount, int totalCandidates, boolean hasSchema) {
        if (parsedCount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal conf = hasSchema ? new BigDecimal("0.75") : new BigDecimal("0.70");
        double ratio = (totalCandidates > 0) ? (double) parsedCount / totalCandidates : 1.0;
        if (ratio >= 0.85) {
            conf = conf.add(new BigDecimal("0.15"));
        } else if (ratio >= 0.60) {
            conf = conf.add(new BigDecimal("0.05"));
        }
        if (parsedCount >= 5) {
            conf = conf.add(new BigDecimal("0.05"));
        }
        return conf.min(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // Positional Parser: DATE CHANNEL MERCHANT CATEGORY DIR AMOUNT NARRATION BALANCE
    // =========================================================================

    /**
     * Deterministically parses a positional transaction line of the format:
     * DATE CHANNEL MERCHANT CATEGORY DEBIT_OR_CREDIT AMOUNT NARRATION BALANCE
     *
     * Example:
     * 2026-10-09 UPI FreshMart Supermarket Groceries Debit 1,845.50 Weekly household groceries 22,654.50
     */
    public ParsedTransaction parsePositionalRow(String line, int rowNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] tokens = line.trim().split("\\s+");
        if (tokens.length < 6) {
            return null;
        }

        // Exclude all-uppercase header rows (e.g. DATE CHANNEL MERCHANT CATEGORY...)
        if (line.equalsIgnoreCase("Date Channel Merchant Category Debit/Credit Amount Narration Balance") ||
            line.equalsIgnoreCase("Date Channel Merchant Category Type Amount Narration Balance")) {
            return null;
        }

        // 1. Token 0: Valid Date
        LocalDate date = parseDateToken(tokens[0]);
        if (date == null) {
            return null;
        }

        // 2. Token 1: Valid Channel (UPI, CARD, IMPS, BBPS, etc.)
        String channel = tokens[1].toUpperCase();
        if (!isKnownChannel(channel)) {
            return null;
        }

        // 3. Find the Direction (Debit / Credit / DR / CR) token index
        int dirIndex = -1;
        for (int i = 2; i < tokens.length - 1; i++) {
            if (isDirectionToken(tokens[i])) {
                dirIndex = i;
                break;
            }
        }

        if (dirIndex == -1 || dirIndex < 3) {
            // Must have at least one token between Channel and Direction (Merchant)
            return null;
        }

        // 4. Token immediately after direction must be a valid Amount
        if (dirIndex + 1 >= tokens.length) {
            return null;
        }
        BigDecimal amount = parseMonetaryAmount(tokens[dirIndex + 1]);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 5. Determine Debit / Credit direction
        String dirToken = tokens[dirIndex].toUpperCase();
        boolean isDebit = dirToken.contains("DEBIT") || dirToken.equals("DR");
        boolean isCredit = dirToken.contains("CREDIT") || dirToken.equals("CR") || dirToken.contains("DEPOSIT");

        if (!isDebit && !isCredit) {
            return null;
        }

        BigDecimal debit = isDebit ? amount : null;
        BigDecimal credit = isCredit ? amount : null;
        TransactionType type = isDebit ? TransactionType.EXPENSE : TransactionType.INCOME;

        // 6. Tokens between channel (index 1) and dirIndex: MERCHANT and optionally CATEGORY
        // If there are 2 or more tokens before dirIndex, last token before dirIndex can be category if known
        String rawMerchantPart;
        String rawCategoryPart = null;

        int midCount = dirIndex - 2;
        if (midCount >= 2) {
            // Check if token at (dirIndex - 1) is a recognizable financial category
            String potentialCategory = tokens[dirIndex - 1];
            if (isRecognizedCategoryToken(potentialCategory)) {
                rawCategoryPart = potentialCategory;
                StringBuilder mb = new StringBuilder();
                for (int i = 2; i < dirIndex - 1; i++) {
                    if (mb.length() > 0) mb.append(" ");
                    mb.append(tokens[i]);
                }
                rawMerchantPart = mb.toString().trim();
            } else {
                StringBuilder mb = new StringBuilder();
                for (int i = 2; i < dirIndex; i++) {
                    if (mb.length() > 0) mb.append(" ");
                    mb.append(tokens[i]);
                }
                rawMerchantPart = mb.toString().trim();
            }
        } else {
            rawMerchantPart = tokens[2];
        }

        // 7. Tokens after amount (index dirIndex + 1): Narration and optional Balance
        // Check if the last token is a valid Balance
        BigDecimal balance = null;
        int narrationEnd = tokens.length;
        if (tokens.length > dirIndex + 2) {
            BigDecimal possibleBal = parseMonetaryAmount(tokens[tokens.length - 1]);
            if (possibleBal != null) {
                balance = possibleBal;
                narrationEnd = tokens.length - 1;
            }
        }

        StringBuilder nb = new StringBuilder();
        for (int i = dirIndex + 2; i < narrationEnd; i++) {
            if (nb.length() > 0) nb.append(" ");
            nb.append(tokens[i]);
        }
        String narration = nb.toString().trim();

        // 8. Description formatting: Combine Merchant and Narration cleanly
        String description;
        if (!narration.isEmpty() && !rawMerchantPart.isEmpty()) {
            description = rawMerchantPart + " - " + narration;
        } else if (!narration.isEmpty()) {
            description = narration;
        } else if (!rawMerchantPart.isEmpty()) {
            description = rawMerchantPart;
        } else {
            description = "Transaction " + rowNumber;
        }

        String txId = "TXN-" + date.toString().replace("-", "") + "-" + String.format("%04d", rowNumber);

        return new ParsedTransaction(
                txId,
                date,
                description,
                debit,
                credit,
                amount,
                type,
                balance,
                rawMerchantPart,
                rawCategoryPart,
                new BigDecimal("0.90"),
                line,
                FORMAT_ID,
                false,
                false,
                false,
                false,
                rawCategoryPart != null
        );
    }

    private static boolean isKnownChannel(String token) {
        if (token == null) return false;
        String upper = token.toUpperCase().replaceAll("[^A-Z]", "");
        return KNOWN_CHANNELS.contains(upper);
    }

    private static boolean isDirectionToken(String token) {
        if (token == null) return false;
        String clean = token.toUpperCase().replaceAll("[^A-Z]", "");
        return clean.equals("DEBIT") || clean.equals("DR") || clean.equals("CREDIT") || clean.equals("CR") || clean.equals("DEPOSIT");
    }

    private static boolean isRecognizedCategoryToken(String token) {
        if (token == null) return false;
        String clean = token.toUpperCase().replaceAll("[^A-Z]", "");
        return clean.equals("GROCERIES") || clean.equals("FOOD") || clean.equals("SHOPPING") ||
               clean.equals("TRANSPORT") || clean.equals("UTILITIES") || clean.equals("ENTERTAINMENT") ||
               clean.equals("HEALTHCARE") || clean.equals("SALARY") || clean.equals("INVESTMENT") ||
               clean.equals("EDUCATION") || clean.equals("BILLS") || clean.equals("RENT");
    }

    // =========================================================================
    // Header & Schema Detection
    // =========================================================================

    private TableSchema findTableSchema(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            TableSchema schema = detectSchemaOnLine(line, i);
            if (schema != null && schema.isValid()) {
                return schema;
            }
        }
        return null;
    }

    private TableSchema detectSchemaOnLine(String line, int lineIndex) {
        List<String> tokens = splitRow(line);
        if (tokens.size() < 3) {
            return null;
        }

        List<ColumnDefinition> cols = new ArrayList<>();
        boolean foundDate = false;
        boolean foundDesc = false;

        for (int c = 0; c < tokens.size(); c++) {
            String token = tokens.get(c).trim();
            ColumnType type = classifyHeaderToken(token);
            if (type == ColumnType.DATE) foundDate = true;
            if (type == ColumnType.DESCRIPTION || type == ColumnType.MERCHANT) foundDesc = true;
            cols.add(new ColumnDefinition(type, c, token));
        }

        if (foundDate && foundDesc) {
            TableSchema schema = new TableSchema(lineIndex, cols);
            if (schema.isValid()) {
                return schema;
            }
        }
        return null;
    }

    private ColumnType classifyHeaderToken(String token) {
        String cleaned = token.replaceAll("[^a-zA-Z0-9/().\\- ]", "").trim();
        if (DATE_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.DATE;
        if (DESC_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.DESCRIPTION;
        if (DEBIT_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.DEBIT;
        if (CREDIT_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.CREDIT;
        if (BALANCE_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.BALANCE;
        if (REF_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.REFERENCE;
        if (AMOUNT_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.AMOUNT;
        if (TYPE_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.TYPE;
        if (CHANNEL_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.CHANNEL;
        if (MERCHANT_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.MERCHANT;
        if (CATEGORY_HEADER_PATTERN.matcher(cleaned).matches()) return ColumnType.CATEGORY;
        return ColumnType.UNKNOWN;
    }

    // =========================================================================
    // Tabular Row Parsing
    // =========================================================================

    private boolean isPotentialTabularRow(String line, TableSchema schema) {
        List<String> tokens = splitRow(line);
        if (tokens.isEmpty()) {
            return false;
        }

        ColumnDefinition dateCol = schema.getColumn(ColumnType.DATE);
        if (dateCol != null && dateCol.index < tokens.size()) {
            LocalDate date = parseDateToken(tokens.get(dateCol.index));
            return date != null;
        }

        LocalDate firstTokenDate = parseDateToken(tokens.get(0));
        return firstTokenDate != null;
    }

    private ParsedTransaction tryParseTabularRow(String line, TableSchema schema, int rowNumber) {
        List<String> tokens = splitRow(line);
        if (tokens.size() < 3) {
            return null;
        }

        ColumnDefinition dateCol = schema.getColumn(ColumnType.DATE);
        ColumnDefinition descCol = schema.getColumn(ColumnType.DESCRIPTION);
        ColumnDefinition balCol = schema.getColumn(ColumnType.BALANCE);

        // 1. Validate and extract date
        LocalDate date = null;
        if (dateCol != null && dateCol.index < tokens.size()) {
            date = parseDateToken(tokens.get(dateCol.index));
        }
        if (date == null) {
            date = parseDateToken(tokens.get(0));
        }
        if (date == null) {
            return null;
        }

        // 2. Extract monetary amounts
        BigDecimal debit = null;
        BigDecimal credit = null;
        BigDecimal amount = BigDecimal.ZERO;
        TransactionType type = null;

        if (schema.hasSeparateDebitCredit) {
            ColumnDefinition debitCol = schema.getColumn(ColumnType.DEBIT);
            ColumnDefinition creditCol = schema.getColumn(ColumnType.CREDIT);

            String debitStr = (debitCol != null && debitCol.index < tokens.size()) ? tokens.get(debitCol.index) : "";
            String creditStr = (creditCol != null && creditCol.index < tokens.size()) ? tokens.get(creditCol.index) : "";

            debit = parseMonetaryAmount(debitStr);
            credit = parseMonetaryAmount(creditStr);

            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                amount = debit;
                type = TransactionType.EXPENSE;
            } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                amount = credit;
                type = TransactionType.INCOME;
            } else {
                return null;
            }
        } else if (schema.hasAmountAndType) {
            ColumnDefinition amountCol = schema.getColumn(ColumnType.AMOUNT);
            ColumnDefinition typeCol = schema.getColumn(ColumnType.TYPE);

            String amtStr = (amountCol != null && amountCol.index < tokens.size()) ? tokens.get(amountCol.index) : "";
            String typeStr = (typeCol != null && typeCol.index < tokens.size()) ? tokens.get(typeCol.index).toUpperCase().trim() : "";

            BigDecimal rawAmt = parseMonetaryAmount(amtStr);
            if (rawAmt == null) {
                return null;
            }

            if (typeStr.contains("DR") || typeStr.contains("DEBIT") || rawAmt.compareTo(BigDecimal.ZERO) < 0) {
                debit = rawAmt.abs();
                amount = debit;
                type = TransactionType.EXPENSE;
            } else if (typeStr.contains("CR") || typeStr.contains("CREDIT") || typeStr.contains("DEPOSIT")) {
                credit = rawAmt.abs();
                amount = credit;
                type = TransactionType.INCOME;
            } else {
                return null;
            }
        } else {
            return null;
        }

        // 3. Extract description
        String description = "";
        if (descCol != null && descCol.index < tokens.size()) {
            description = tokens.get(descCol.index).trim();
        }
        if (description.isEmpty()) {
            description = extractDescriptionFallback(tokens, schema);
        }
        if (description.isEmpty()) {
            description = "Transaction " + rowNumber;
        }

        // 4. Extract balance (optional)
        BigDecimal balance = null;
        if (balCol != null && balCol.index < tokens.size()) {
            balance = parseMonetaryAmount(tokens.get(balCol.index));
        }

        String txId = "TXN-" + date.toString().replace("-", "") + "-" + String.format("%04d", rowNumber);

        return new ParsedTransaction(
                txId,
                date,
                description,
                debit,
                credit,
                amount,
                type,
                balance,
                null,
                null,
                new BigDecimal("0.85"),
                line,
                FORMAT_ID,
                false,
                false,
                false,
                false
        );
    }

    private String extractDescriptionFallback(List<String> tokens, TableSchema schema) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (parseDateToken(token) != null || parseMonetaryAmount(token) != null) {
                continue;
            }
            if (token.equalsIgnoreCase("DR") || token.equalsIgnoreCase("CR") || token.equalsIgnoreCase("DEBIT") || token.equalsIgnoreCase("CREDIT")) {
                continue;
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(token);
        }
        return sb.toString().trim();
    }

    private boolean isContinuationLine(String line) {
        List<String> tokens = splitRow(line);
        if (tokens.isEmpty()) {
            return false;
        }
        if (parseDateToken(tokens.get(0)) != null) {
            return false;
        }
        int amountCount = 0;
        for (String t : tokens) {
            if (parseMonetaryAmount(t) != null) {
                amountCount++;
            }
        }
        return amountCount <= 1;
    }

    private boolean isIndependentRejectedRow(String line) {
        String lower = line.toLowerCase();
        // If row contains words indicating an independent entity/record or malformed uppercase data
        return lower.contains("malformed")
                || lower.contains("invalid")
                || lower.contains("rejected")
                || lower.contains("account no")
                || lower.contains("invoice");
    }

    private boolean isHeaderOrFooterLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("page ")
                || lower.contains("page of")
                || lower.startsWith("total")
                || lower.contains("statement summary")
                || lower.contains("generated on")
                || lower.contains("customer care")
                || lower.contains("disclaimer")
                || lower.contains("account statement");
    }

    // =========================================================================
    // Value Parsing & Normalization
    // =========================================================================

    public static LocalDate parseDateToken(String token) {
        if (token == null) return null;
        String cleaned = token.replaceAll("[^0-9a-zA-Z/\\- ]", "").trim();
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, dtf);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public static BigDecimal parseMonetaryAmount(String token) {
        if (token == null) return null;
        String trimmed = token.trim();
        if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.equals("--")) {
            return null;
        }

        Matcher m = AMOUNT_PATTERN.matcher(trimmed);
        if (m.matches()) {
            String numStr = m.group(1).replace(",", "");
            try {
                BigDecimal val = new BigDecimal(numStr);
                if (trimmed.startsWith("-")) {
                    val = val.negate();
                }
                return val.setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal extractOpeningBalance(String[] lines, int headerLineIndex) {
        for (int i = 0; i <= headerLineIndex && i < lines.length; i++) {
            Matcher m = OPENING_BALANCE_PATTERN.matcher(lines[i]);
            if (m.find()) {
                String num = m.group(1).replace(",", "");
                try {
                    return new BigDecimal(num).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private BigDecimal extractClosingBalance(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            Matcher m = CLOSING_BALANCE_PATTERN.matcher(lines[i]);
            if (m.find()) {
                String num = m.group(1).replace(",", "");
                try {
                    return new BigDecimal(num).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private List<String> splitRow(String line) {
        if (line == null) return Collections.emptyList();
        if (line.contains("|")) {
            String[] parts = line.split("\\|");
            List<String> list = new ArrayList<>();
            for (String p : parts) list.add(p.trim());
            return list;
        } else if (line.contains("\t")) {
            String[] parts = line.split("\t");
            List<String> list = new ArrayList<>();
            for (String p : parts) list.add(p.trim());
            return list;
        } else {
            String[] parts = line.split("\\s{2,}");
            if (parts.length > 1) {
                List<String> list = new ArrayList<>();
                for (String p : parts) list.add(p.trim());
                return list;
            } else {
                String[] words = line.split("\\s+");
                return Arrays.asList(words);
            }
        }
    }

    // =========================================================================
    // Financial Reconciliation
    // =========================================================================

    private void performReconciliation(List<ParsedTransaction> txns,
                                       BigDecimal openingBalance,
                                       BigDecimal closingBalance,
                                       List<String> warnings) {
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (ParsedTransaction tx : txns) {
            if (tx.getCredit() != null && tx.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                totalCredits = totalCredits.add(tx.getCredit());
            }
            if (tx.getDebit() != null && tx.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                totalDebits = totalDebits.add(tx.getDebit());
            }
        }

        if (openingBalance != null && closingBalance != null) {
            BigDecimal computedClosing = openingBalance.add(totalCredits).subtract(totalDebits);
            if (computedClosing.compareTo(closingBalance) != 0) {
                BigDecimal diff = computedClosing.subtract(closingBalance).abs();
                warnings.add(String.format(Locale.ENGLISH,
                        "Statement reconciliation discrepancy: Opening (%s) + Credits (%s) - Debits (%s) = %s, but stated Closing Balance is %s (Discrepancy: %s)",
                        openingBalance, totalCredits, totalDebits, computedClosing, closingBalance, diff));
            }
        } else {
            warnings.add("Opening and/or Closing balances not explicitly declared; balance reconciliation could not be verified.");
        }

        BigDecimal previousBalance = openingBalance;
        for (int i = 0; i < txns.size(); i++) {
            ParsedTransaction tx = txns.get(i);
            BigDecimal rowBal = tx.getBalance();
            if (rowBal != null && previousBalance != null) {
                BigDecimal expected = previousBalance;
                if (tx.getCredit() != null && tx.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                    expected = expected.add(tx.getCredit());
                } else if (tx.getDebit() != null && tx.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                    expected = expected.subtract(tx.getDebit());
                }
                if (expected.compareTo(rowBal) != 0) {
                    warnings.add(String.format(Locale.ENGLISH,
                            "Running balance discrepancy at row %d ('%s'): expected %s but reported %s",
                            i + 1, tx.getDescription(), expected, rowBal));
                }
            }
            if (rowBal != null) {
                previousBalance = rowBal;
            }
        }
    }
}
