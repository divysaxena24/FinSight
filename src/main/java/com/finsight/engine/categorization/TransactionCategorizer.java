package com.finsight.engine.categorization;

import com.finsight.engine.model.ParsedTransaction;

/**
 * Interface contract for transaction categorization.
 */
public interface TransactionCategorizer {

    /**
     * Evaluates a transaction and returns an immutable CategorizationResult.
     * Does NOT mutate the input transaction.
     */
    CategorizationResult categorize(ParsedTransaction transaction);

    /**
     * Applies categorization directly onto a transaction, respecting manual correction flags.
     * If transaction.isManuallyCategorized() is true, existing category is strictly preserved.
     */
    void applyCategorization(ParsedTransaction transaction);
}

