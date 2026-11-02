package com.stockflow;

import com.stockflow.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("StockFlow — Smart Retail Inventory & Store Management System");
        SceneNavigator.init(stage);
        SceneNavigator.showLogin();
    }
}
