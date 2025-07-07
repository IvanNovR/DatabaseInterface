package ru.ivannovr.dbinterface.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.ivannovr.dbinterface.model.TableSchema;
import ru.ivannovr.dbinterface.service.DatabaseService;
import ru.ivannovr.dbinterface.utils.CSVExporter;
import ru.ivannovr.dbinterface.utils.DeleteConfirmationFormatter;
import ru.ivannovr.dbinterface.utils.FxmlLoader;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TableViewController extends BaseController {
    private static final double MIN_WINDOW_WIDTH = 800.0;
    private static final double WINDOW_HEIGHT = 600.0;
    private static final double MAX_WINDOW_WIDTH = 1200.0;
    private static final double COLUMN_WIDTH = 125.0;
    private static final double PADDING = 40.0;

    private DatabaseService databaseService;
    private ObservableList<TableSchema> tables = FXCollections.observableArrayList();
    private ObservableList<ObservableList<String>> tableData = FXCollections.observableArrayList();

    @FXML
    private ComboBox<TableSchema> tableComboBox;
    @FXML
    private TableView<ObservableList<String>> dataTable;
    @FXML
    private TextField searchField;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button logoutButton;

    public void setDatabaseConnection(DatabaseService databaseService) {
        this.databaseService = databaseService;
        initializeTables();
    }

    @FXML
    private void initialize() {
        tableComboBox.setItems(tables);
        tableComboBox.setCellFactory(param -> new ListCell<TableSchema>() {
            @Override
            protected void updateItem(TableSchema item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        tableComboBox.setButtonCell(new ListCell<TableSchema>() {
            @Override
            protected void updateItem(TableSchema item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        tableComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTableData(newVal);
            }
        });

        tableComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !tables.isEmpty()) {
                tableComboBox.getSelectionModel().selectFirst();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        addButton.setOnAction(e -> openRecordDialog(false, null));
        editButton.setOnAction(e -> {
            ObservableList<String> selectedRow = dataTable.getSelectionModel().getSelectedItem();
            if (selectedRow == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please select a row to edit");
                return;
            }
            openRecordDialog(true, selectedRow);
        });
        deleteButton.setOnAction(e -> handleDelete());
        exportButton.setOnAction(e -> handleExportToCSV());
        logoutButton.setOnAction(e -> handleLogout());
        logger.debug("TableViewController initialized, ComboBox items: {}", tables);
    }

    private void initializeTables() {
        try {
            tables.setAll(databaseService.getTables());
            logger.debug("Tables loaded: {}", tables);
            if (tables.isEmpty()) {
                logger.warn("No tables loaded");
                showAlert(Alert.AlertType.WARNING, "Warning", "No tables found in the database");
            }
        } catch (SQLException e) {
            logger.error("Failed to load tables", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load tables: " + e.getMessage());
        }
    }

    private void loadTableData(TableSchema table) {
        try {
            dataTable.getColumns().clear();
            List<String> columns = table.getColumns();
            logger.debug("Loading columns for table {}: {}", table.getName(), columns);
            for (int i = 0; i < columns.size(); i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columns.get(i));
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    return new SimpleStringProperty(row != null && colIndex < row.size() ? row.get(colIndex) : "");
                });
                column.setPrefWidth(COLUMN_WIDTH);
                dataTable.getColumns().add(column);
            }

            tableData.setAll(databaseService.getTableData(table.getName()));
            dataTable.setItems(tableData);
            logger.debug("Table data loaded: {} rows", tableData.size());

            // Настройка размера окна
            adjustWindowSize(table);
            logger.info("Loaded data for table: {}", table.getName());
        } catch (SQLException e) {
            logger.error("Failed to load table data for {}", table.getName(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load table data: " + e.getMessage());
        }
    }

    private void adjustWindowSize(TableSchema table) {
        if (dataTable.getScene() == null) {
            logger.warn("Scene is null in adjustWindowSize for table {}", table.getName());
            return;
        }
        Stage stage = (Stage) dataTable.getScene().getWindow();
        double prefWidth = Math.max(MIN_WINDOW_WIDTH, table.getColumns().size() * COLUMN_WIDTH + PADDING);
        prefWidth = Math.min(prefWidth, MAX_WINDOW_WIDTH);

        stage.setWidth(prefWidth);
        stage.setHeight(WINDOW_HEIGHT);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        stage.centerOnScreen();
    }

    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        TableSchema selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
        if (selectedTable == null) return;

        try {
            if (searchText.isEmpty()) {
                loadTableData(selectedTable);
            } else {
                tableData.setAll(databaseService.searchTableData(selectedTable.getName(), searchText));
                dataTable.setItems(tableData);
                adjustWindowSize(selectedTable); // Пересчитываем размер после поиска
            }
        } catch (SQLException e) {
            logger.error("Search failed for table {}", selectedTable.getName(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Search failed: " + e.getMessage());
        }
    }

    private void openRecordDialog(boolean isEditMode, ObservableList<String> selectedRow) {
        TableSchema selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
        if (selectedTable == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a table");
            return;
        }

        try {
            FxmlLoader.SceneAndController<RecordDialogController> result = FxmlLoader.loadSceneAndController(
                    "/ru/ivannovr/dbinterface/ui/record-dialog-view.fxml", 400, 600);
            Scene scene = result.getScene();
            scene.getStylesheets().add(getClass().getResource("/ru/ivannovr/dbinterface/ui/styles.css").toExternalForm());
            RecordDialogController controller = result.getController();
            controller.setDatabaseService(databaseService);
            controller.setTable(selectedTable);
            controller.setEditMode(isEditMode, selectedRow);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(isEditMode ? "Edit Record" : "Add Record");
            dialog.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
            dialog.setScene(scene);
            controller.populateFields();
            controller.adjustDialogSize(); // Настройка размера диалога
            dialog.showAndWait();

            loadTableData(selectedTable);
        } catch (IOException e) {
            logger.error("Failed to load add/edit dialog", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dialog: " + e.getMessage());
        }
    }

    private void handleDelete() {
        TableSchema selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
        ObservableList<String> selectedRow = dataTable.getSelectionModel().getSelectedItem();
        if (selectedTable == null || selectedRow == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a table and a row");
            return;
        }

        try {
            Map<String, List<ObservableList<String>>> relatedRecords = databaseService.findRelatedRecords(
                    selectedTable.getName(), selectedTable.getColumns(), selectedRow);
            String message = DeleteConfirmationFormatter.formatConfirmationMessage(selectedRow, relatedRecords);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/ru/ivannovr/dbinterface/ui/styles.css").toExternalForm());
            confirm.setTitle("Confirm Cascade Delete");
            confirm.setHeaderText("Confirm Deletion of Record and Related Data");
            confirm.getDialogPane().setPrefWidth(600);
            Stage alertStage = (Stage) confirm.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        databaseService.cascadeDelete(selectedTable.getName(), selectedTable.getColumns(), selectedRow);
                        loadTableData(selectedTable);
                        logger.info("Deleted record from table {} with cascade", selectedTable.getName());
                    } catch (SQLException e) {
                        logger.error("Failed to delete record from table {}", selectedTable.getName(), e);
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete record: " + e.getMessage());
                    }
                }
            });
        } catch (SQLException e) {
            logger.error("Failed to find related records for table {}", selectedTable.getName(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to check related records: " + e.getMessage());
        }
    }

    private void handleExportToCSV() {
        TableSchema selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
        if (selectedTable == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a table to export");
            return;
        }

        try {
            ObservableList<ObservableList<String>> data = databaseService.getTableData(selectedTable.getName());
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV File");
            fileChooser.setInitialFileName(selectedTable.getName() + ".csv");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            Stage stage = (Stage) exportButton.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file == null) {
                logger.debug("Export cancelled by user for table {}", selectedTable.getName());
                return;
            }

            CSVExporter.exportToCSV(selectedTable, data, file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Table " + selectedTable.getName() + " exported to " + file.toPath());
        } catch (SQLException e) {
            logger.error("Failed to export table {} to CSV: {}", selectedTable.getName(), e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to export table: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to write CSV file for table {}: {}", selectedTable.getName(), e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to write CSV file: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            FxmlLoader.SceneAndController<ConnectionController> result = FxmlLoader.loadSceneAndController(
                    "/ru/ivannovr/dbinterface/ui/connection-view.fxml", 600, 600);
            Scene scene = result.getScene();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Database Interface");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
            stage.setScene(scene);
            stage.show();
            logger.info("Logged out, returned to connection view");
        } catch (IOException e) {
            logger.error("Failed to load connection view", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to return to login screen: " + e.getMessage());
        }
    }
}