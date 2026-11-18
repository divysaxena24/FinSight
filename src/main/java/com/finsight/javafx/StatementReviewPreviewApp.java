package com.finsight.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.finsight.engine.StatementAnalysisEngine;
import com.finsight.engine.StatementAnalysisResult;
import com.finsight.engine.model.FinancialStatementReport;
import com.finsight.engine.model.ParsedTransaction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

/**
 * Optional read-only JavaFX utility for previewing statement analysis.
 *
 * This class is not referenced by Tomcat, Servlets, JSPs, or application startup.
 * Run it separately with: mvn javafx:run
 */
public final class StatementReviewPreviewApp extends Application {

    private final StatementAnalysisEngine analysisEngine = new StatementAnalysisEngine();
    private final ObservableList<StatementPreviewRow> allRows = FXCollections.observableArrayList();
    private final Label selectedFileLabel = new Label("No PDF selected");
    private final Label statusLabel = new Label("Choose a digital bank statement PDF to begin.");
    private final Label summaryLabel = new Label("No analysis loaded");
    private final Button analyzeButton = new Button("Analyze Preview");
    private final TableView<StatementPreviewRow> transactionTable = new TableView<>();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private Path selectedPdf;

    @Override
    public void start(Stage stage) {
        Button chooseButton = new Button("Choose PDF");
        chooseButton.setOnAction(event -> choosePdf(stage));

        analyzeButton.setDisable(true);
        analyzeButton.setOnAction(event -> analyzeSelectedPdf());

        categoryFilter.getItems().add("All Categories");
        categoryFilter.setValue("All Categories");
        categoryFilter.setOnAction(event -> applyCategoryFilter());

        configureTable();

        HBox actions = new HBox(10, chooseButton, analyzeButton, categoryFilter);
        actions.setPadding(new Insets(12));

        VBox header = new VBox(6,
                new Label("FinSight Statement Review Preview"),
                new Label("Read-only JavaFX preview using the existing statement analysis engine."),
                selectedFileLabel,
                summaryLabel,
                statusLabel);
        header.setPadding(new Insets(16, 16, 8, 16));

        BorderPane root = new BorderPane();
        root.setTop(new VBox(header, actions));
        root.setCenter(transactionTable);
        BorderPane.setMargin(transactionTable, new Insets(0, 16, 16, 16));

        Scene scene = new Scene(root, 1050, 650);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> handleKeyPress(event, stage));
        stage.setTitle("FinSight - Statement Review Preview");
        stage.setScene(scene);
        stage.show();
    }

    private void choosePdf(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Bank Statement PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPdf = file.toPath();
            selectedFileLabel.setText("Selected: " + file.getName());
            statusLabel.setText("Ready to analyze the selected PDF.");
            analyzeButton.setDisable(false);
        }
    }

    private void analyzeSelectedPdf() {
        Path pdf = Objects.requireNonNull(selectedPdf, "A PDF must be selected before analysis.");
        setBusy(true);
        statusLabel.setText("Analyzing statement...");

        Task<StatementAnalysisResult> task = new Task<>() {
            @Override
            protected StatementAnalysisResult call() throws IOException {
                try (InputStream input = Files.newInputStream(pdf)) {
                    return analysisEngine.analyze(input);
                }
            }
        };

        task.setOnSucceeded(event -> showAnalysis(task.getValue()));
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            statusLabel.setText("Analysis failed: " + failure.getMessage());
            setBusy(false);
        });

        Thread analysisThread = new Thread(task, "finsight-statement-analysis");
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    private void showAnalysis(StatementAnalysisResult result) {
        setBusy(false);
        if (!result.isSuccessful() || result.getFinancialReport() == null) {
            statusLabel.setText("Analysis failed: " + valueOrFallback(
                    result.getFailureMessage(), result.getStatus().name()));
            allRows.clear();
            transactionTable.refresh();
            return;
        }

        FinancialStatementReport report = result.getFinancialReport();
        allRows.setAll(report.getTransactions().stream()
                .map(StatementPreviewRow::from)
                .toList());

        categoryFilter.getItems().setAll("All Categories");
        report.getCategoryExpenses().keySet().stream()
                .sorted()
                .forEach(category -> categoryFilter.getItems().add(category));
        categoryFilter.setValue("All Categories");
        applyCategoryFilter();

        summaryLabel.setText(String.format(
                "Bank: %s | Transactions: %d | Income: %s | Expenses: %s",
                valueOrFallback(result.getBankName(), "Unknown"),
                result.getParsedTransactionCount(),
                report.getSummary().getTotalIncome().toPlainString(),
                report.getSummary().getNetExpenses().toPlainString()));
        statusLabel.setText("Analysis complete. Preview only; no database changes were made.");
    }

    private void configureTable() {
        transactionTable.setPlaceholder(new Label("Analyzed transactions will appear here."));
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addColumn("Date", "date", 110);
        addColumn("Description", "description", 260);
        addColumn("Merchant", "merchant", 180);
        addColumn("Category", "category", 150);
        addColumn("Type", "type", 100);
        addColumn("Amount", "amount", 120);
        transactionTable.setItems(new FilteredList<>(allRows));
    }

    private void addColumn(String title, String property, double width) {
        TableColumn<StatementPreviewRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        transactionTable.getColumns().add(column);
    }

    @SuppressWarnings("unchecked")
    private void applyCategoryFilter() {
        if (!(transactionTable.getItems() instanceof FilteredList<?> filtered)) {
            return;
        }

        FilteredList<StatementPreviewRow> rows = (FilteredList<StatementPreviewRow>) filtered;
        String selectedCategory = categoryFilter.getValue();
        rows.setPredicate(row -> selectedCategory == null
                || "All Categories".equals(selectedCategory)
                || selectedCategory.equals(row.getCategory()));
    }

    private void handleKeyPress(KeyEvent event, Stage stage) {
        if (event.isControlDown() && event.getCode() == KeyCode.O) {
            choosePdf(stage);
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            Platform.exit();
        }
    }

    private void setBusy(boolean busy) {
        analyzeButton.setDisable(busy || selectedPdf == null);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
