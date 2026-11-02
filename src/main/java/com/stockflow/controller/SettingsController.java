package com.stockflow.controller;

import com.stockflow.config.DatabaseConfig;
import com.stockflow.exception.AppException;
import com.stockflow.model.User;
import com.stockflow.service.AuthService;
import com.stockflow.util.Alerts;
import com.stockflow.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class SettingsController {
    @FXML private Label accountLabel, messageLabel, dbLabel;
    @FXML private PasswordField oldField, newField, confirmField;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        User u = Session.user();
        accountLabel.setText("Name: " + u.getFullName() + "\nUsername: " + u.getUsername() + "\nRole: " + u.getRole());
        String url = "";
        try {
            url = DatabaseConfig.getUrl();
        } catch (RuntimeException ignored) {
        }
        dbLabel.setText(Session.user().getRole() == com.stockflow.model.Role.ADMIN ? "Database: " + url : "");
    }

    @FXML
    private void onChangePassword() {
        try {
            auth.changePassword(oldField.getText(), newField.getText(), confirmField.getText());
            oldField.clear();
            newField.clear();
            confirmField.clear();
            message("Password updated successfully.", false);
        } catch (AppException e) {
            message(e.getMessage(), true);
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void message(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("form-error", "form-success");
        messageLabel.getStyleClass().add(error ? "form-error" : "form-success");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
