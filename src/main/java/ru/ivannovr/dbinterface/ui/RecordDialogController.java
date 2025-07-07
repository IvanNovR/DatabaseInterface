package ru.ivannovr.dbinterface.ui;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.ivannovr.dbinterface.model.TableSchema;
import ru.ivannovr.dbinterface.service.DatabaseService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecordDialogController extends BaseController {
    private static final double MIN_DIALOG_WIDTH = 400.0;
    private static final double MIN_DIALOG_HEIGHT = 300.0;
    private static final double MAX_DIALOG_HEIGHT = 1000.0;
    private static final double FIELD_HEIGHT = 80.0; // Label + TextField
    private static final double PADDING = 40.0;

    private DatabaseService databaseService;
    private TableSchema table;
    private List<String> currentRowData;
    private boolean isEditMode;

    @FXML
    private VBox fieldsVBox;
    @FXML
    private Button saveButton;

    private List<TextField> inputFields;

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void setTable(TableSchema table) {
        this.table = table;
    }

    public void setEditMode(boolean editMode, List<String> rowData) {
        this.isEditMode = editMode;
        this.currentRowData = rowData;
    }

    @FXML
    private void initialize() {
        inputFields = new ArrayList<>();
        saveButton.setOnAction(e -> handleSave());
        logger.debug("RecordDialogController initialized");
    }

    public void populateFields() {
        fieldsVBox.getChildren().clear();
        inputFields.clear();

        List<String> autoIncrementColumns = table.getAutoIncrementColumns();
        for (int i = 0; i < table.getColumns().size(); i++) {
            String column = table.getColumns().get(i);
            if (autoIncrementColumns.contains(column)) {
                continue;
            }
            Label label = new Label(column + ":");
            TextField textField = new TextField();
            textField.setPromptText("Enter " + column);
            if (isEditMode && currentRowData != null && i < currentRowData.size()) {
                textField.setText(currentRowData.get(i));
            }
            fieldsVBox.getChildren().addAll(label, textField);
            inputFields.add(textField);
        }

        logger.debug("Populated fields for {} mode on table {}", isEditMode ? "edit" : "add", table.getName());
        adjustDialogSize(); // Настройка размера после заполнения полей
    }

    private void handleSave() {
        try {
            List<String> nonAutoColumns = new ArrayList<>();
            List<String> values = new ArrayList<>();
            List<String> notNullColumns = table.getNotNullColumns();
            List<String> missingFields = new ArrayList<>();
            int inputFieldIndex = 0;

            for (String column : table.getColumns()) {
                if (!table.getAutoIncrementColumns().contains(column)) {
                    nonAutoColumns.add(column);
                    String value = inputFields.get(inputFieldIndex).getText();
                    if (notNullColumns.contains(column) && (value == null || value.trim().isEmpty())) {
                        missingFields.add(column);
                    }
                    values.add(value);
                    inputFieldIndex++;
                }
            }

            if (!missingFields.isEmpty()) {
                String errorMessage = "Please fill in the following required fields: " + String.join(", ", missingFields);
                showAlert(Alert.AlertType.ERROR, "Error", errorMessage);
                logger.warn("Failed to save: missing required fields {}", missingFields);
                return;
            }

            logger.debug("Non-auto columns: {}, Values: {}", nonAutoColumns, values);

            if (isEditMode) {
                List<String> fullValues = new ArrayList<>();
                inputFieldIndex = 0;
                for (int i = 0; i < table.getColumns().size(); i++) {
                    String column = table.getColumns().get(i);
                    if (table.getAutoIncrementColumns().contains(column)) {
                        fullValues.add(currentRowData != null && i < currentRowData.size() ? currentRowData.get(i) : null);
                    } else {
                        fullValues.add(inputFields.get(inputFieldIndex).getText());
                        inputFieldIndex++;
                    }
                }
                databaseService.updateRecord(table.getName(), table.getColumns(), fullValues, currentRowData);
                logger.info("Updated record in table {}", table.getName());
            } else {
                databaseService.addRecord(table.getName(), nonAutoColumns, values);
                logger.info("Added new record to table {}", table.getName());
            }

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Operation failed: " + e.getMessage());
            logger.error("Failed to {} record in table {}", isEditMode ? "update" : "add", table.getName(), e);
        }
    }

    public void adjustDialogSize() {
        if (fieldsVBox.getScene() == null) {
            logger.warn("Scene is null in adjustDialogSize for table {}", table.getName());
            return;
        }
        Stage stage = (Stage) fieldsVBox.getScene().getWindow();
        double prefWidth = MIN_DIALOG_WIDTH;
        double prefHeight = fieldsVBox.getChildren().size() * FIELD_HEIGHT / 2 + PADDING + 50.0; // Учитываем заголовок и кнопку
        prefHeight = Math.max(MIN_DIALOG_HEIGHT, prefHeight);
        prefHeight = Math.min(prefHeight, MAX_DIALOG_HEIGHT);

        stage.setWidth(prefWidth);
        stage.setHeight(prefHeight);
        stage.setMinWidth(MIN_DIALOG_WIDTH);
        stage.setMinHeight(MIN_DIALOG_HEIGHT);
        stage.centerOnScreen();

        // Добавляем ScrollPane, если содержимое превышает максимальную высоту
        if (prefHeight >= MAX_DIALOG_HEIGHT) {
            ScrollPane scrollPane = new ScrollPane(fieldsVBox);
            scrollPane.setFitToWidth(true);
            Scene scene = new Scene(scrollPane, prefWidth, MAX_DIALOG_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/ru/ivannovr/dbinterface/ui/styles.css").toExternalForm());
            stage.setScene(scene);
        }
    }
}