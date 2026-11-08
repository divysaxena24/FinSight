package com.finsight.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.finsight.engine.insights.BudgetRecommendation;
import com.finsight.engine.insights.FinancialInsight;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.parser.StatementParseResult;
import com.finsight.engine.pdf.PdfExtractionResult;

/**
 * Immutable result model encapsulating the complete end-to-end outcome of a bank statement analysis.
 * Exposes extraction status, parser routing provenance, parsed transaction metrics,
 * deterministic analytics, historical baselines, financial insights, budget recommendations,
 * and traceable warnings.
 */
public class StatementAnalysisResult {

    public enum Status {
        SUCCESS,
        PARTIALLY_PARSED,
        UNSUPPORTED_FORMAT,
        AMBIGUOUS_FORMAT,
        EXTRACTION_FAILED,
        PARSING_FAILED,
        EMPTY_INPUT
    }

    private final Status status;
    private final boolean suitableForReview;
    private final PdfExtractionResult.Status extractionStatus;
    private final StatementParseResult.Status parseStatus;
    private final String parserFormatId;
    private final String bankName;
    private final BigDecimal parserDetectionConfidence;

    private final FinancialStatementReport financialReport;
    private final Map<String, BigDecimal> spendingBaselines;
    private final List<FinancialInsight> financialInsights;
    private final List<BudgetRecommendation> budgetRecommendations;

    private final int parsedTransactionCount;
    private final int unparsedLineCount;
    private final List<String> unparsedLines;

    private final List<String> extractionWarnings;
    private final List<String> parserWarnings;
    private final List<String> analysisWarnings;

    private final LocalDate declaredStartDate;
    private final LocalDate declaredEndDate;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;
    private final String failureMessage;
    private final int historicalCoverageMonths;
    private final boolean sufficientHistoryForBudgets;

    public StatementAnalysisResult(Status status,
                                   boolean suitableForReview,
                                   PdfExtractionResult.Status extractionStatus,
                                   StatementParseResult.Status parseStatus,
                                   String parserFormatId,
                                   String bankName,
                                   BigDecimal parserDetectionConfidence,
                                   FinancialStatementReport financialReport,
                                   Map<String, BigDecimal> spendingBaselines,
                                   List<FinancialInsight> financialInsights,
                                   List<BudgetRecommendation> budgetRecommendations,
                                   int parsedTransactionCount,
                                   int unparsedLineCount,
                                   List<String> unparsedLines,
                                   List<String> extractionWarnings,
                                   List<String> parserWarnings,
                                   List<String> analysisWarnings,
                                   LocalDate declaredStartDate,
                                   LocalDate declaredEndDate,
                                   LocalDate effectiveStartDate,
                                   LocalDate effectiveEndDate,
                                   String failureMessage) {
        this(status, suitableForReview, extractionStatus, parseStatus, parserFormatId, bankName,
                parserDetectionConfidence, financialReport, spendingBaselines, financialInsights,
                budgetRecommendations, parsedTransactionCount, unparsedLineCount, unparsedLines,
                extractionWarnings, parserWarnings, analysisWarnings, declaredStartDate,
                declaredEndDate, effectiveStartDate, effectiveEndDate, failureMessage, 0, false);
    }

    public StatementAnalysisResult(Status status,
                                   boolean suitableForReview,
                                   PdfExtractionResult.Status extractionStatus,
                                   StatementParseResult.Status parseStatus,
                                   String parserFormatId,
                                   String bankName,
                                   BigDecimal parserDetectionConfidence,
                                   FinancialStatementReport financialReport,
                                   Map<String, BigDecimal> spendingBaselines,
                                   List<FinancialInsight> financialInsights,
                                   List<BudgetRecommendation> budgetRecommendations,
                                   int parsedTransactionCount,
                                   int unparsedLineCount,
                                   List<String> unparsedLines,
                                   List<String> extractionWarnings,
                                   List<String> parserWarnings,
                                   List<String> analysisWarnings,
                                   LocalDate declaredStartDate,
                                   LocalDate declaredEndDate,
                                   LocalDate effectiveStartDate,
                                   LocalDate effectiveEndDate,
                                   String failureMessage,
                                   int historicalCoverageMonths,
                                   boolean sufficientHistoryForBudgets) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.suitableForReview = suitableForReview;
        this.extractionStatus = extractionStatus;
        this.parseStatus = parseStatus;
        this.parserFormatId = parserFormatId;
        this.bankName = bankName;
        this.parserDetectionConfidence = parserDetectionConfidence != null ? parserDetectionConfidence : BigDecimal.ZERO;
        this.financialReport = financialReport;
        this.spendingBaselines = spendingBaselines != null ? Collections.unmodifiableMap(spendingBaselines) : Collections.emptyMap();
        this.financialInsights = financialInsights != null ? Collections.unmodifiableList(financialInsights) : Collections.emptyList();
        this.budgetRecommendations = budgetRecommendations != null ? Collections.unmodifiableList(budgetRecommendations) : Collections.emptyList();
        this.parsedTransactionCount = parsedTransactionCount;
        this.unparsedLineCount = unparsedLineCount;
        this.unparsedLines = unparsedLines != null ? Collections.unmodifiableList(unparsedLines) : Collections.emptyList();
        this.extractionWarnings = extractionWarnings != null ? Collections.unmodifiableList(extractionWarnings) : Collections.emptyList();
        this.parserWarnings = parserWarnings != null ? Collections.unmodifiableList(parserWarnings) : Collections.emptyList();
        this.analysisWarnings = analysisWarnings != null ? Collections.unmodifiableList(analysisWarnings) : Collections.emptyList();
        this.declaredStartDate = declaredStartDate;
        this.declaredEndDate = declaredEndDate;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.failureMessage = failureMessage;
        this.historicalCoverageMonths = historicalCoverageMonths;
        this.sufficientHistoryForBudgets = sufficientHistoryForBudgets;
    }

    public static StatementAnalysisResult extractionFailure(PdfExtractionResult extractionResult) {
        Status st = (extractionResult.getStatus() == PdfExtractionResult.Status.EMPTY_INPUT)
                ? Status.EMPTY_INPUT : Status.EXTRACTION_FAILED;
        return new StatementAnalysisResult(
                st,
                false,
                extractionResult.getStatus(),
                null,
                null,
                null,
                BigDecimal.ZERO,
                null,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0,
                Collections.emptyList(),
                extractionResult.getWarnings(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                extractionResult.getErrorMessage()
        );
    }

    public static StatementAnalysisResult routingOrParseFailure(Status status,
                                                               PdfExtractionResult.Status extractionStatus,
                                                               StatementParseResult.Status parseStatus,
                                                               String parserFormatId,
                                                               String bankName,
                                                               BigDecimal confidence,
                                                               String errorMessage,
                                                               List<String> extractionWarnings,
                                                               List<String> parserWarnings) {
        return new StatementAnalysisResult(
                status,
                false,
                extractionStatus,
                parseStatus,
                parserFormatId,
                bankName,
                confidence,
                null,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0,
                Collections.emptyList(),
                extractionWarnings,
                parserWarnings,
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                errorMessage
        );
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS || status == Status.PARTIALLY_PARSED;
    }

    public boolean isSuitableForReview() {
        return suitableForReview;
    }

    public PdfExtractionResult.Status getExtractionStatus() {
        return extractionStatus;
    }

    public StatementParseResult.Status getParseStatus() {
        return parseStatus;
    }

    public String getParserFormatId() {
        return parserFormatId;
    }

    public String getBankName() {
        return bankName;
    }

    public BigDecimal getParserDetectionConfidence() {
        return parserDetectionConfidence;
    }

    public FinancialStatementReport getFinancialReport() {
        return financialReport;
    }

    public Map<String, BigDecimal> getSpendingBaselines() {
        return spendingBaselines;
    }

    public List<FinancialInsight> getFinancialInsights() {
        return financialInsights;
    }

    public List<BudgetRecommendation> getBudgetRecommendations() {
        return budgetRecommendations;
    }

    public int getParsedTransactionCount() {
        return parsedTransactionCount;
    }

    public int getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public List<String> getUnparsedLines() {
        return unparsedLines;
    }

    public List<String> getExtractionWarnings() {
        return extractionWarnings;
    }

    public List<String> getParserWarnings() {
        return parserWarnings;
    }

    public List<String> getAnalysisWarnings() {
        return analysisWarnings;
    }

    public LocalDate getDeclaredStartDate() {
        return declaredStartDate;
    }

    public LocalDate getDeclaredEndDate() {
        return declaredEndDate;
    }

    public LocalDate getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public LocalDate getEffectiveEndDate() {
        return effectiveEndDate;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public int getHistoricalCoverageMonths() {
        return historicalCoverageMonths;
    }

    public boolean isSufficientHistoryForBudgets() {
        return sufficientHistoryForBudgets;
    }

    @Override
    public String toString() {
        return "StatementAnalysisResult{" +
                "status=" + status +
                ", suitableForReview=" + suitableForReview +
                ", bankName='" + bankName + '\'' +
                ", parsedTransactions=" + parsedTransactionCount +
                ", unparsedLines=" + unparsedLineCount +
                '}';
    }
}

