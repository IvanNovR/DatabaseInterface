package ru.ivannovr.dbinterface.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ru.ivannovr.dbinterface.database.DatabaseConnection;
import ru.ivannovr.dbinterface.model.DatabaseType;
import ru.ivannovr.dbinterface.service.DatabaseService;
import ru.ivannovr.dbinterface.utils.FxmlLoader;

import java.io.IOException;
import java.util.Objects;

public class ConnectionController extends BaseController {
    private static final double MIN_WINDOW_WIDTH = 400.0;
    private static final double MIN_WINDOW_HEIGHT = 400.0;
    private static final double PREF_WINDOW_WIDTH = 500.0;
    private static final double PREF_WINDOW_HEIGHT = 450.0;

    @FXML
    private ComboBox<DatabaseType> dbTypeComboBox;
    @FXML
    private TextField serverField;
    @FXML
    private TextField databaseField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button connectButton;

    @FXML
    private void initialize() {
        dbTypeComboBox.setItems(FXCollections.observableArrayList(DatabaseType.values()));
        dbTypeComboBox.getSelectionModel().select(DatabaseType.MSSQL);
        connectButton.setOnAction(e -> handleConnect());
        logger.debug("ConnectionController initialized, dbTypeComboBox set with items: {}", DatabaseType.values());
    }

    @FXML
    private void handleConnect() {
        DatabaseType dbType = dbTypeComboBox.getSelectionModel().getSelectedItem();
        String server = serverField.getText().trim();
        String database = databaseField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (dbType == null || server.isEmpty() || database.isEmpty() || username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields (Database Type, Server, Database, Username)");
            return;
        }

        try {
            DatabaseConnection connection = dbType.createConnection(server, database, username, password);
            DatabaseService databaseService = dbType.createDatabaseService(connection);

            FxmlLoader.SceneAndController<TableViewController> result = FxmlLoader.loadSceneAndController(
                    "/ru/ivannovr/dbinterface/ui/table-view.fxml", 800, 600);
            Scene scene = result.getScene();
            scene.getStylesheets().add(getClass().getResource("/ru/ivannovr/dbinterface/ui/styles.css").toExternalForm());
            TableViewController controller = result.getController();
            controller.setDatabaseConnection(databaseService);

            Stage stage = (Stage) connectButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Database Interface - Main");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
            stage.centerOnScreen();
            stage.show();

            logger.info("Connected to {} database, switched to main view", dbType.getDisplayName());
        } catch (IOException e) {
            logger.error("Failed to load main view", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load main view: " + e.getMessage());
        }
    }
}