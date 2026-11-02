package com.stockflow.controller;

import com.stockflow.model.Role;
import com.stockflow.model.User;
import com.stockflow.service.UserService;
import com.stockflow.util.*;
import com.stockflow.util.TableUtil.Action;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class UsersController {
    @FXML private TextField searchField;
    @FXML private TableView<User> table;
    @FXML private Label countLabel;

    private final UserService service = new UserService();

    @FXML
    private void initialize() {
        TableUtil.setup(table, "No users found.");
        table.getColumns().addAll(
                TableUtil.text("Username", 150, User::getUsername),
                TableUtil.text("Full Name", 220, User::getFullName),
                TableUtil.text("Role", 100, u -> u.getRole().name()),
                TableUtil.status("Status", 100, u -> u.isActive() ? "Active" : "Inactive"),
                TableUtil.text("Created", 130, u -> Fmt.dateTime(u.getCreatedAt())),
                TableUtil.actions(330,
                        Action.of("Edit", "btn-secondary", this::openEdit),
                        Action.of("Reset Password", "btn-secondary", this::openReset),
                        new Action<User>(u -> u.isActive() ? "Deactivate" : "Activate", "btn-danger-outline", this::toggle, u -> true)));
        searchField.textProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        try {
            List<User> list = service.search(searchField.getText().trim());
            table.getItems().setAll(list);
            countLabel.setText(list.size() + " user(s)");
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    @FXML
    private void onAdd() {
        FormDialog d = new FormDialog("Add User", "Create");
        TextField username = d.text("Username *", "");
        TextField name = d.text("Full name *", "");
        ComboBox<Role> role = d.add("Role *", new ComboBox<>(FXCollections.observableArrayList(Role.values())));
        role.setValue(Role.STAFF);
        PasswordField pw = d.add("Password *", new PasswordField());
        PasswordField pw2 = d.add("Confirm password *", new PasswordField());
        if (d.show(() -> service.create(username.getText(), name.getText(), role.getValue(), pw.getText(), pw2.getText()))) refresh();
    }

    private void openEdit(User u) {
        FormDialog d = new FormDialog("Edit User — " + u.getUsername(), "Save");
        TextField name = d.text("Full name *", u.getFullName());
        ComboBox<Role> role = d.add("Role *", new ComboBox<>(FXCollections.observableArrayList(Role.values())));
        role.setValue(u.getRole());
        if (d.show(() -> service.update(u, name.getText(), role.getValue()))) refresh();
    }

    private void openReset(User u) {
        FormDialog d = new FormDialog("Reset password — " + u.getUsername(), "Reset");
        PasswordField pw = d.add("New password *", new PasswordField());
        PasswordField pw2 = d.add("Confirm password *", new PasswordField());
        if (d.show(() -> service.resetPassword(u, pw.getText(), pw2.getText()))) {
            Alerts.info("Password reset", "Password updated for " + u.getUsername() + ".");
        }
    }

    private void toggle(User u) {
        boolean activate = !u.isActive();
        if (!activate && !Alerts.confirm("Deactivate user", "Deactivate \"" + u.getUsername() + "\"?\nThey will no longer be able to log in.")) return;
        try {
            service.setActive(u, activate);
            refresh();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
            refresh();
        }
    }
}
