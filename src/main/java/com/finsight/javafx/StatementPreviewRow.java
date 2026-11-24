package com.finsight.javafx;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.finsight.engine.model.ParsedTransaction;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Read-only table representation for the optional JavaFX statement preview.
 */
public final class StatementPreviewRow {

    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty merchant = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty amount = new SimpleStringProperty();

    private StatementPreviewRow(ParsedTransaction transaction) {
        LocalDate transactionDate = transaction.getTransactionDate();
        BigDecimal transactionAmount = transaction.getAmount();

        date.set(transactionDate != null ? transactionDate.toString() : "");
        description.set(valueOrEmpty(transaction.getDescription()));
        merchant.set(valueOrEmpty(transaction.getMerchant()));
        category.set(valueOrEmpty(transaction.getCategory()));
        type.set(transaction.getTransactionType() != null
                ? transaction.getTransactionType().name()
                : "");
        amount.set(transactionAmount != null ? transactionAmount.toPlainString() : "0.00");
    }

    public static StatementPreviewRow from(ParsedTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("transaction must not be null");
        }
        return new StatementPreviewRow(transaction);
    }

    public String getDate() {
        return date.get();
    }

    public String getDescription() {
        return description.get();
    }

    public String getMerchant() {
        return merchant.get();
    }

    public String getCategory() {
        return category.get();
    }

    public String getType() {
        return type.get();
    }

    public String getAmount() {
        return amount.get();
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
