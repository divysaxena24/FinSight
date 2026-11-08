package com.finsight.engine;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.finsight.engine.analytics.AnalyticsCalculator;
import com.finsight.engine.categorization.RuleBasedTransactionCategorizer;
import com.finsight.engine.categorization.TransactionCategorizer;
import com.finsight.engine.insights.BudgetRecommendation;
import com.finsight.engine.insights.BudgetRecommendationEngine;
import com.finsight.engine.insights.FinancialInsight;
import com.finsight.engine.insights.SpendingBaselineCalculator;
import com.finsight.engine.insights.UnusualTransactionDetector;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.parser.BankStatementParser;
import com.finsight.engine.parser.StatementParseResult;
import com.finsight.engine.parser.StatementParserRouter;
import com.finsight.engine.parser.StatementParserRouter.RoutingDecision;
import com.finsight.engine.pdf.PdfExtractionResult;
import com.finsight.engine.pdf.SecurePdfExtractor;

/**
 * End-to-end Statement Analysis Orchestration Engine.
 * 
 * Pipeline:
 * SecurePdfExtractor → StatementParserRouter → TransactionCategorizer → AnalyticsCalculator
 *                   → SpendingBaselineCalculator / UnusualTransactionDetector / BudgetRecommendationEngine
 * 
 * Orchestrates deterministic processing from raw PDF streams to unified financial intelligence.
 */
public class StatementAnalysisEngine {

    private final SecurePdfExtractor pdfExtractor;
    private final StatementParserRouter parserRouter;
    private final TransactionCategorizer categorizer;
    private final AnalyticsCalculator analyticsCalculator;
    private final SpendingBaselineCalculator baselineCalculator;
    private final UnusualTransactionDetector unusualDetector;
    private final BudgetRecommendationEngine budgetEngine;

    public StatementAnalysisEngine() {
        this(new SecurePdfExtractor(),
             new StatementParserRouter(),
             new RuleBasedTransactionCategorizer(),
             new AnalyticsCalculator(),
             new SpendingBaselineCalculator(),
             new UnusualTransactionDetector(),
             new BudgetRecommendationEngine());
    }

    public StatementAnalysisEngine(SecurePdfExtractor pdfExtractor,
                                  StatementParserRouter parserRouter,
                                  TransactionCategorizer categorizer,
                                  AnalyticsCalculator analyticsCalculator,
                                  SpendingBaselineCalculator baselineCalculator,
                                  UnusualTransactionDetector unusualDetector,
                                  BudgetRecommendationEngine budgetEngine) {
        this.pdfExtractor = Objects.requireNonNull(pdfExtractor, "pdfExtractor must not be null");
        this.parserRouter = Objects.requireNonNull(parserRouter, "parserRouter must not be null");
        this.categorizer = Objects.requireNonNull(categorizer, "categorizer must not be null");
        this.analyticsCalculator = Objects.requireNonNull(analyticsCalculator, "analyticsCalculator must not be null");
        this.baselineCalculator = Objects.requireNonNull(baselineCalculator, "baselineCalculator must not be null");
        this.unusualDetector = Objects.requireNonNull(unusualDetector, "unusualDetector must not be null");
        this.budgetEngine = Objects.requireNonNull(budgetEngine, "budgetEngine must not be null");
    }

    /**
     * Executes end-to-end statement analysis without declared statement boundaries.
     */
    public StatementAnalysisResult analyze(InputStream pdfInputStream) {
        return analyze(pdfInputStream, null, null, null);
    }

    /**
     * Executes end-to-end statement analysis with declared statement boundary dates.
     */
    public StatementAnalysisResult analyze(InputStream pdfInputStream,
                                           LocalDate statementStart,
                                           LocalDate statementEnd) {
        return analyze(pdfInputStream, statementStart, statementEnd, null);
    }

    /**
     * Executes end-to-end statement analysis with declared boundaries and prior historical context.
     * 
     * @param pdfInputStream stream of uploaded bank statement PDF
     * @param statementStart optional declared start date
     * @param statementEnd optional declared end date
     * @param priorHistoricalTransactions optional prior user transactions for richer multi-month intelligence
     * @return unified immutable StatementAnalysisResult
     */
    public StatementAnalysisResult analyze(InputStream pdfInputStream,
                                           LocalDate statementStart,
                                           LocalDate statementEnd,
                                           List<ParsedTransaction> priorHistoricalTransactions) {
        if (pdfInputStream == null) {
            PdfExtractionResult emptyResult = PdfExtractionResult.emptyInput();
            return StatementAnalysisResult.extractionFailure(emptyResult);
        }

        // 1. PDF Text Extraction
        PdfExtractionResult extractionResult = pdfExtractor.extract(pdfInputStream);
        if (!extractionResult.isSuccess()) {
            return StatementAnalysisResult.extractionFailure(extractionResult);
        }

        String extractedText = extractionResult.getText();

        // 2. Parser Routing
        RoutingDecision routingDecision = parserRouter.route(extractedText);
        if (!routingDecision.isRouted()) {
            StatementAnalysisResult.Status failStatus;
            if (routingDecision.getStatus() == StatementParseResult.Status.AMBIGUOUS_FORMAT) {
                failStatus = StatementAnalysisResult.Status.AMBIGUOUS_FORMAT;
            } else if (routingDecision.getStatus() == StatementParseResult.Status.EMPTY_INPUT) {
                failStatus = StatementAnalysisResult.Status.EMPTY_INPUT;
            } else {
                failStatus = StatementAnalysisResult.Status.UNSUPPORTED_FORMAT;
            }

            return StatementAnalysisResult.routingOrParseFailure(
                    failStatus,
                    extractionResult.getStatus(),
                    routingDecision.getStatus(),
                    null,
                    null,
                    routingDecision.getConfidence(),
                    routingDecision.getFailureReason(),
                    extractionResult.getWarnings(),
                    Collections.emptyList()
            );
        }

        BankStatementParser selectedParser = routingDecision.getSelectedParser();
        BigDecimal detectionConfidence = routingDecision.getConfidence();

        // 3. Statement Parsing
        StatementParseResult parseResult = selectedParser.parse(extractedText);
        if (parseResult.getStatus() == StatementParseResult.Status.PARSING_FAILURE) {
            return StatementAnalysisResult.routingOrParseFailure(
                    StatementAnalysisResult.Status.PARSING_FAILED,
                    extractionResult.getStatus(),
                    parseResult.getStatus(),
                    selectedParser.getFormatId(),
                    selectedParser.getBankName(),
                    detectionConfidence,
                    parseResult.getErrorMessage(),
                    extractionResult.getWarnings(),
                    parseResult.getWarnings()
            );
        }

        List<ParsedTransaction> rawTransactions = parseResult.getTransactions();
        List<String> unparsedLines = parseResult.getRawUnparsedLines();

        if (rawTransactions == null || rawTransactions.isEmpty()) {
            // No usable transactions extracted from the recognized statement
            return StatementAnalysisResult.routingOrParseFailure(
                    StatementAnalysisResult.Status.PARSING_FAILED,
                    extractionResult.getStatus(),
                    parseResult.getStatus(),
                    selectedParser.getFormatId(),
                    selectedParser.getBankName(),
                    detectionConfidence,
                    "Statement was recognized as " + selectedParser.getBankName() 
                            + ", but no valid transactions could be parsed.",
                    extractionResult.getWarnings(),
                    parseResult.getWarnings()
            );
        }

        // 4. Deterministic Categorization
        // Defensive copy to prevent mutation of parser-owned collections
        List<ParsedTransaction> categorizedTransactions = new ArrayList<>();
        for (ParsedTransaction tx : rawTransactions) {
            // Create independent clone of transaction before categorization
            ParsedTransaction clonedTx = new ParsedTransaction(tx);
            categorizer.applyCategorization(clonedTx);
            categorizedTransactions.add(clonedTx);
        }

        // 5. Financial Statement Report & Analytics
        FinancialStatementReport report = analyticsCalculator.calculateReport(
                categorizedTransactions, statementStart, statementEnd);

        // Derive consistent effective boundaries for intelligence components
        LocalDate effectiveStart = report.getEffectiveStartDate() != null ? report.getEffectiveStartDate() : statementStart;
        LocalDate effectiveEnd = report.getEffectiveEndDate() != null ? report.getEffectiveEndDate() : statementEnd;

        // Combine current statement transactions with prior history for intelligence (if provided)
        List<ParsedTransaction> combinedIntelligenceTransactions = new ArrayList<>(categorizedTransactions);
        if (priorHistoricalTransactions != null && !priorHistoricalTransactions.isEmpty()) {
            for (ParsedTransaction priorTx : priorHistoricalTransactions) {
                if (priorTx != null) {
                    combinedIntelligenceTransactions.add(new ParsedTransaction(priorTx));
                }
            }
        }

        // Determine effective historical coverage months
        List<java.time.YearMonth> fullEligibleMonths = baselineCalculator.getEligibleFullMonths(
                combinedIntelligenceTransactions, statementStart, statementEnd);
        int coverageMonths = !fullEligibleMonths.isEmpty() ? fullEligibleMonths.size() : 0;
        if (coverageMonths == 0) {
            // Count unique YearMonths present in data
            coverageMonths = (int) combinedIntelligenceTransactions.stream()
                    .map(tx -> tx.getTransactionDate() != null ? java.time.YearMonth.from(tx.getTransactionDate()) : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        }

        boolean sufficientHistory = coverageMonths >= baselineCalculator.getMinMonthsForBaseline();

        // 6. Historical Spending Baselines
        Map<String, BigDecimal> baselines = baselineCalculator.calculateCategoryBaselines(
                combinedIntelligenceTransactions, statementStart, statementEnd);

        // 7. Unusual Transaction Detection
        List<FinancialInsight> insights = unusualDetector.detectAll(
                combinedIntelligenceTransactions, statementStart, statementEnd);

        // 8. Budget Recommendations
        List<BudgetRecommendation> budgetRecommendations = budgetEngine.generateRecommendations(
                combinedIntelligenceTransactions, statementStart, statementEnd);

        // 9. Assemble Unified Result
        StatementAnalysisResult.Status finalStatus = (unparsedLines != null && !unparsedLines.isEmpty())
                ? StatementAnalysisResult.Status.PARTIALLY_PARSED
                : StatementAnalysisResult.Status.SUCCESS;

        List<String> analysisWarnings = new ArrayList<>(report.getWarnings());

        return new StatementAnalysisResult(
                finalStatus,
                true, // Suitable for user review
                extractionResult.getStatus(),
                parseResult.getStatus(),
                selectedParser.getFormatId(),
                selectedParser.getBankName(),
                detectionConfidence,
                report,
                baselines,
                insights,
                budgetRecommendations,
                categorizedTransactions.size(),
                unparsedLines != null ? unparsedLines.size() : 0,
                unparsedLines,
                extractionResult.getWarnings(),
                parseResult.getWarnings(),
                analysisWarnings,
                statementStart,
                statementEnd,
                effectiveStart,
                effectiveEnd,
                null,
                coverageMonths,
                sufficientHistory
        );
    }

    public SecurePdfExtractor getPdfExtractor() {
        return pdfExtractor;
    }

    public StatementParserRouter getParserRouter() {
        return parserRouter;
    }

    public TransactionCategorizer getCategorizer() {
        return categorizer;
    }

    public AnalyticsCalculator getAnalyticsCalculator() {
        return analyticsCalculator;
    }

    public SpendingBaselineCalculator getBaselineCalculator() {
        return baselineCalculator;
    }

    public UnusualTransactionDetector getUnusualDetector() {
        return unusualDetector;
    }

    public BudgetRecommendationEngine getBudgetEngine() {
        return budgetEngine;
    }
}
