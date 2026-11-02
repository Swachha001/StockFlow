package com.stockflow.controller;

import com.stockflow.model.ActivityLog;
import com.stockflow.service.ActivityService;
import com.stockflow.util.Alerts;
import com.stockflow.util.Fmt;
import com.stockflow.util.TableUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class ActivityLogsController {
    @FXML private TextField searchField;
    @FXML private TableView<ActivityLog> table;
    @FXML private Label countLabel;

    @FXML
    private void initialize() {
        TableUtil.setup(table, "No activity recorded yet.");
        table.getColumns().addAll(
                TableUtil.text("Time", 140, l -> Fmt.dateTime(l.getCreatedAt())),
                TableUtil.text("User", 120, ActivityLog::getUsername),
                TableUtil.text("Action", 180, ActivityLog::getAction),
                TableUtil.text("Details", 520, ActivityLog::getDetails));
        searchField.textProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    @FXML
    private void refresh() {
        try {
            List<ActivityLog> list = ActivityService.search(searchField.getText().trim());
            table.getItems().setAll(list);
            countLabel.setText(list.size() + " entr" + (list.size() == 1 ? "y" : "ies"));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }
}
