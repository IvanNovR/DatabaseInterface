package ru.ivannovr.dbinterface.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.App;

import java.io.IOException;

public class FxmlLoader {
    private static final Logger logger = LogManager.getLogger(FxmlLoader.class);
    private static final String CSS_PATH = "/ru/ivannovr/dbinterface/ui/styles.css";

    public static class SceneAndController<T> {
        private final Scene scene;
        private final T controller;

        public SceneAndController(Scene scene, T controller) {
            this.scene = scene;
            this.controller = controller;
        }

        public Scene getScene() {
            return scene;
        }

        public T getController() {
            return controller;
        }
    }

    public static <T> SceneAndController<T> loadSceneAndController(String fxmlPath, double width, double height) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath));
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            scene.getStylesheets().add(FxmlLoader.class.getResource(CSS_PATH).toExternalForm());
            T controller = fxmlLoader.getController();
            logger.debug("Loaded scene and controller from {} with CSS {}", fxmlPath, CSS_PATH);
            return new SceneAndController<>(scene, controller);
        } catch (IOException e) {
            logger.error("Failed to load scene and controller from {}", fxmlPath, e);
            throw e;
        }
    }
}