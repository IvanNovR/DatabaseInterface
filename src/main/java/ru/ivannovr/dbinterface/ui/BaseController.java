package ru.ivannovr.dbinterface.ui;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public abstract class BaseController {
    protected final Logger logger;

    protected BaseController() {
        this.logger = LogManager.getLogger(this.getClass());
    }

    protected void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/ru/ivannovr/dbinterface/ui/styles.css").toExternalForm());
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
        alert.showAndWait();
    }
}