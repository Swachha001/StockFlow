package com.stockflow.controller;

import com.stockflow.exception.AppException;
import com.stockflow.exception.DatabaseException;
import com.stockflow.service.AuthService;
import com.stockflow.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.prefs.Preferences;

public class LoginController {
    private static final String PREF_USER = "lastUsername";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private Label errorLabel;

    private final AuthService auth = new AuthService();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private void initialize() {
        String saved = prefs.get(PREF_USER, "");
        if (!saved.isEmpty()) {
            usernameField.setText(saved);
            rememberMe.setSelected(true);
            Platform.runLater(passwordField::requestFocus);
        }
    }

    @FXML
    private void onLogin() {
        hideError();
        String user = usernameField.getText();
        String pass = passwordField.getText();
        if (user == null || user.isBlank() || pass == null || pass.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }
        try {
            auth.login(user, pass);
            if (rememberMe.isSelected()) prefs.put(PREF_USER, user.trim()); else prefs.remove(PREF_USER);
            passwordField.clear();
            SceneNavigator.showMain();
        } catch (AppException e) {
            showError(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        } catch (DatabaseException e) {
            e.printStackTrace();
            String m = e.getMessage() == null ? "" : e.getMessage();
            showError(m.startsWith("Database settings")
                    ? m
                    : "Cannot connect to the database. Make sure MySQL is running and db.properties (user/password/URL) is correct.\n\nDetails: " + m.lines().findFirst().orElse(""));
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
