package ru.ivannovr.dbinterface;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.ui.TableViewController;
import ru.ivannovr.dbinterface.utils.FxmlLoader;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {
    private static final Logger logger = LogManager.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting DatabaseInterface application");
            FxmlLoader.SceneAndController<TableViewController> result = FxmlLoader.loadSceneAndController(
                    "/ru/ivannovr/dbinterface/ui/connection-view.fxml", 600, 600);
            Scene scene = result.getScene();
            primaryStage.setTitle("Database Interface");
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/ru/ivannovr/dbinterface/images/icon.png"))));
            primaryStage.setScene(scene);
            primaryStage.sizeToScene();
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Failed to load connection view", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}