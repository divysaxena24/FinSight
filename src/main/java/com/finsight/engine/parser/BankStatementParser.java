package com.finsight.engine.parser;

import java.math.BigDecimal;

/**
 * Contract for bank statement parsers.
 * Each implementation specializes in a specific bank's e-statement layout or tabular structure.
 */
public interface BankStatementParser {

    /**
     * Unique identifier for this bank format (e.g. "HDFC_RETAIL_V1", "SBI_SAVINGS_V1").
     */
    String getFormatId();

    /**
     * Human-readable bank or institution name (e.g. "HDFC Bank", "State Bank of India").
     */
    String getBankName();

    /**
     * Evaluates format detection confidence against the raw extracted text.
     * Evaluates structural tabular headers, layout indicators, and format patterns.
     * Must return 0.00 if structural criteria are not met, avoiding naive keyword-only matching.
     *
     * @param statementText raw extracted text from the digital PDF statement
     * @return confidence score between 0.00 and 1.00
     */
    BigDecimal detectConfidence(String statementText);

    /**
     * Parses the extracted statement text into structured ParsedTransaction instances.
     *
     * @param statementText raw extracted text from the statement
     * @return StatementParseResult containing parsed rows, rejected lines, and warnings
     */
    StatementParseResult parse(String statementText);
}

