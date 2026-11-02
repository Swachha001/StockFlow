package com.stockflow.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Loads FXML views and switches between the login and main scenes. */
public final class SceneNavigator {
    private static final String VIEW_PATH = "/com/stockflow/view/";
    private static final String CSS = "/css/stockflow.css";
    private static Stage stage;

    private SceneNavigator() { }

    public static void init(Stage primary) {
        stage = primary;
    }

    public static String css() {
        return Objects.requireNonNull(SceneNavigator.class.getResource(CSS), "stockflow.css missing").toExternalForm();
    }

    public static <T extends Parent> T load(String viewName) {
        return load(viewName, null);
    }

    /** Loads /com/stockflow/view/{viewName}.fxml; pass a controller only for views without fx:controller. */
    public static <T extends Parent> T load(String viewName, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    SceneNavigator.class.getResource(VIEW_PATH + viewName + ".fxml"), viewName + ".fxml missing"));
            if (controller != null) loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load view " + viewName, e);
        }
    }

    public static void showLogin() {
        show(load("LoginView"), 900, 580, false);
    }

    public static void showMain() {
        // Fit the window to the screen: on a 1366x768 laptop (or with 125-150% Windows scaling) a fixed 1280x780
        // window would hang off the bottom of the screen and hide the buttons at the bottom of every page.
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1280, screen.getWidth() - 20);
        double h = Math.min(780, screen.getHeight() - 60); // 60 = title bar + window border
        show(load("MainView"), w, h, true);
    }

    private static void show(Parent root, double w, double h, boolean resizable) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(css());
        stage.setMaximized(false);
        stage.setScene(scene);
        stage.setMinWidth(resizable ? Math.min(1000, w) : w);
        stage.setMinHeight(resizable ? Math.min(640, h) : h);
        stage.setResizable(resizable);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }
}
