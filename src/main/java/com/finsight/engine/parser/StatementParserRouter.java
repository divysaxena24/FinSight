package com.finsight.engine.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Statement Parser Router.
 * 
 * Responsibilities:
 * 1. Maintains registry of candidate BankStatementParser implementations.
 * 2. Evaluates statement text across all registered parsers to compute detection confidence.
 * 3. Enforces detection thresholds (minimum 0.70 confidence).
 * 4. Disambiguates candidate parsers; rejects ambiguous cases when two parsers score within 0.05.
 * 5. Rejects unsupported or empty formats explicitly rather than silently misparsing.
 */
public class StatementParserRouter {

    public static final BigDecimal MIN_DETECTION_THRESHOLD = new BigDecimal("0.70");
    public static final BigDecimal AMBIGUITY_DELTA = new BigDecimal("0.05");

    private final List<BankStatementParser> registeredParsers;

    public static class RoutingDecision {
        private final boolean routed;
        private final BankStatementParser selectedParser;
        private final BigDecimal confidence;
        private final StatementParseResult.Status status;
        private final String failureReason;
        private final List<CandidateScore> evaluatedCandidates;

        public RoutingDecision(boolean routed,
                               BankStatementParser selectedParser,
                               BigDecimal confidence,
                               StatementParseResult.Status status,
                               String failureReason,
                               List<CandidateScore> evaluatedCandidates) {
            this.routed = routed;
            this.selectedParser = selectedParser;
            this.confidence = confidence != null ? confidence : BigDecimal.ZERO;
            this.status = status;
            this.failureReason = failureReason;
            this.evaluatedCandidates = evaluatedCandidates != null 
                    ? Collections.unmodifiableList(evaluatedCandidates) 
                    : Collections.emptyList();
        }

        public boolean isRouted() {
            return routed;
        }

        public BankStatementParser getSelectedParser() {
            return selectedParser;
        }

        public BigDecimal getConfidence() {
            return confidence;
        }

        public StatementParseResult.Status getStatus() {
            return status;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public List<CandidateScore> getEvaluatedCandidates() {
            return evaluatedCandidates;
        }
    }

    public static class CandidateScore implements Comparable<CandidateScore> {
        private final BankStatementParser parser;
        private final BigDecimal confidence;

        public CandidateScore(BankStatementParser parser, BigDecimal confidence) {
            this.parser = parser;
            this.confidence = confidence != null ? confidence : BigDecimal.ZERO;
        }

        public BankStatementParser getParser() {
            return parser;
        }

        public BigDecimal getConfidence() {
            return confidence;
        }

        @Override
        public int compareTo(CandidateScore o) {
            // Sort descending by confidence
            return o.confidence.compareTo(this.confidence);
        }
    }

    public StatementParserRouter() {
        this.registeredParsers = new ArrayList<>();
        this.registeredParsers.add(new GenericBankStatementParser());
    }

    public static StatementParserRouter createEmpty() {
        return new StatementParserRouter(Collections.emptyList());
    }

    public StatementParserRouter(List<BankStatementParser> parsers) {
        this.registeredParsers = new ArrayList<>();
        if (parsers != null) {
            this.registeredParsers.addAll(parsers);
        }
    }

    /**
     * Registers a candidate parser.
     */
    public synchronized void registerParser(BankStatementParser parser) {
        if (parser != null && !registeredParsers.contains(parser)) {
            registeredParsers.add(parser);
        }
    }

    /**
     * Unregisters a candidate parser.
     */
    public synchronized boolean unregisterParser(BankStatementParser parser) {
        return registeredParsers.remove(parser);
    }

    public List<BankStatementParser> getRegisteredParsers() {
        return Collections.unmodifiableList(registeredParsers);
    }

    /**
     * Inspects extracted text and determines the appropriate parser.
     * Does NOT execute parsing; strictly selects and returns routing decision.
     */
    public RoutingDecision route(String statementText) {
        if (statementText == null || statementText.trim().isEmpty()) {
            return new RoutingDecision(
                    false,
                    null,
                    BigDecimal.ZERO,
                    StatementParseResult.Status.EMPTY_INPUT,
                    "Statement text is empty or null.",
                    Collections.emptyList()
            );
        }

        if (registeredParsers.isEmpty()) {
            return new RoutingDecision(
                    false,
                    null,
                    BigDecimal.ZERO,
                    StatementParseResult.Status.UNSUPPORTED_FORMAT,
                    "No statement parsers are registered on the platform.",
                    Collections.emptyList()
            );
        }

        List<CandidateScore> scores = new ArrayList<>();
        for (BankStatementParser parser : registeredParsers) {
            try {
                BigDecimal confidence = parser.detectConfidence(statementText);
                if (confidence != null && confidence.compareTo(BigDecimal.ZERO) > 0) {
                    scores.add(new CandidateScore(parser, confidence));
                }
            } catch (Exception e) {
                // If a parser throws during detection, treat confidence as 0 and continue
            }
        }

        if (scores.isEmpty()) {
            return new RoutingDecision(
                    false,
                    null,
                    BigDecimal.ZERO,
                    StatementParseResult.Status.UNSUPPORTED_FORMAT,
                    "Statement format not recognized. No registered parser matched structural layout.",
                    Collections.emptyList()
            );
        }

        Collections.sort(scores);
        CandidateScore best = scores.get(0);

        // Check minimum confidence threshold
        if (best.getConfidence().compareTo(MIN_DETECTION_THRESHOLD) < 0) {
            return new RoutingDecision(
                    false,
                    null,
                    best.getConfidence(),
                    StatementParseResult.Status.UNSUPPORTED_FORMAT,
                    "Confidence score (" + best.getConfidence() + ") is below minimum required threshold (" 
                            + MIN_DETECTION_THRESHOLD + ") for bank " + best.getParser().getBankName(),
                    scores
            );
        }

        // Check for ambiguous match between top candidates
        if (scores.size() > 1) {
            CandidateScore runnerUp = scores.get(1);
            BigDecimal diff = best.getConfidence().subtract(runnerUp.getConfidence()).abs();
            if (diff.compareTo(AMBIGUITY_DELTA) <= 0 && runnerUp.getConfidence().compareTo(MIN_DETECTION_THRESHOLD) >= 0) {
                return new RoutingDecision(
                        false,
                        null,
                        best.getConfidence(),
                        StatementParseResult.Status.AMBIGUOUS_FORMAT,
                        "Ambiguous statement format: Closely matched both [" + best.getParser().getBankName() 
                                + "] (" + best.getConfidence() + ") and [" + runnerUp.getParser().getBankName() 
                                + "] (" + runnerUp.getConfidence() + ").",
                        scores
                );
            }
        }

        return new RoutingDecision(
                true,
                best.getParser(),
                best.getConfidence(),
                StatementParseResult.Status.SUCCESS,
                null,
                scores
        );
    }

    /**
     * High-level coordinator: routes and executes parsing using the selected parser.
     */
    public StatementParseResult parse(String statementText) {
        RoutingDecision decision = route(statementText);
        if (!decision.isRouted()) {
            return StatementParseResult.failure(
                    decision.getStatus(),
                    "UNKNOWN",
                    decision.getConfidence(),
                    decision.getFailureReason(),
                    Collections.emptyList()
            );
        }

        return decision.getSelectedParser().parse(statementText);
    }
}

