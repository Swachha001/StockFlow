package com.stockflow.controller;

import com.stockflow.model.Contact;
import com.stockflow.model.ReportModels.HistoryRow;
import com.stockflow.util.*;
import com.stockflow.util.TableUtil.Action;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared screen logic for Suppliers and Customers (same fields, different rules and history). */
public abstract class ContactController<T extends Contact> {
    @FXML private Label titleLabel, subtitleLabel, countLabel;
    @FXML private Button addButton;
    @FXML private TextField searchField;
    @FXML private CheckBox showInactive;
    @FXML private TableView<T> table;

    protected abstract String singular();
    protected abstract String plural();
    protected abstract String subtitle();
    protected abstract T newContact();
    protected abstract List<T> load(String query, boolean includeInactive);
    protected abstract void save(T contact);
    protected abstract void setActive(T contact, boolean active);
    protected abstract List<HistoryRow> history(T contact);
    protected abstract String historyTitle();
    protected abstract boolean canAdd();
    protected abstract boolean canManage();

    @FXML
    public void initialize() {
        titleLabel.setText(plural());
        subtitleLabel.setText(subtitle());
        addButton.setText("+ Add " + singular());
        addButton.setVisible(canAdd());
        addButton.setManaged(canAdd());
        TableUtil.setup(table, "No " + plural().toLowerCase() + " found.");
        table.getColumns().addAll(
                TableUtil.text("Name", 200, Contact::getName),
                TableUtil.text("Phone", 120, Contact::getPhone),
                TableUtil.text("Email", 200, Contact::getEmail),
                TableUtil.text("Address", 260, Contact::getAddress),
                TableUtil.status("Status", 90, c -> c.isActive() ? "Active" : "Inactive"),
                TableUtil.actions(canManage() ? 250 : 90,
                        Action.of("View", "btn-secondary", this::showDetails),
                        Action.<T>of("Edit", "btn-secondary", c -> openForm(singular(), c, this::save, this::refresh), c -> canManage()),
                        new Action<T>(c -> c.isActive() ? "Deactivate" : "Activate", "btn-danger-outline", this::toggle, c -> canManage())));
        searchField.textProperty().addListener((o, a, b) -> refresh());
        showInactive.selectedProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        try {
            List<T> list = load(searchField.getText().trim(), showInactive.isSelected());
            table.getItems().setAll(list);
            countLabel.setText(list.size() + " " + (list.size() == 1 ? singular() : plural()).toLowerCase());
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    @FXML
    private void onAdd() {
        openForm(singular(), newContact(), this::save, this::refresh);
    }

    private void toggle(T c) {
        boolean activate = !c.isActive();
        if (!activate && !Alerts.confirm("Deactivate " + singular().toLowerCase(), "Deactivate \"" + c.getName() + "\"?")) return;
        try {
            setActive(c, activate);
            refresh();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void showDetails(T c) {
        try {
            TableView<HistoryRow> hist = new TableView<>();
            TableUtil.setup(hist, "No " + historyTitle().toLowerCase() + " yet.");
            hist.getColumns().addAll(
                    TableUtil.text("Reference", 110, HistoryRow::ref),
                    TableUtil.text("Date", 130, r -> Fmt.dateTime(r.date())),
                    TableUtil.money("Amount", 130, HistoryRow::amount),
                    TableUtil.text("By", 150, HistoryRow::extra));
            List<HistoryRow> rows = history(c);
            hist.getItems().setAll(rows);
            hist.setPrefHeight(220);
            hist.setPrefWidth(560);
            BigDecimal total = rows.stream().map(HistoryRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            Label title = new Label(historyTitle() + " (" + rows.size() + ")  ·  Total " + Fmt.money(total));
            title.getStyleClass().add("panel-title");
            new DetailsDialog(singular() + " Details")
                    .row("Name", c.getName()).row("Phone", c.getPhone()).row("Email", c.getEmail())
                    .row("Address", c.getAddress()).row("Status", c.isActive() ? "Active" : "Inactive")
                    .row("Added", Fmt.dateTime(c.getCreatedAt()))
                    .node(title).node(hist).show();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    /** Reusable add/edit popup; also used by the POS screen for quick "new customer". */
    public static <C extends Contact> boolean openForm(String singular, C contact, Consumer<C> saver, Runnable afterSave) {
        boolean edit = contact.getId() != 0;
        FormDialog d = new FormDialog((edit ? "Edit " : "Add ") + singular, "Save");
        TextField name = d.text("Name *", contact.getName());
        TextField phone = d.text("Phone *", contact.getPhone());
        TextField email = d.text("Email", contact.getEmail());
        TextField address = d.text("Address", contact.getAddress());
        phone.setPromptText("e.g. 01712345678");
        boolean saved = d.show(() -> {
            // work on the object directly; validation errors leave the dialog open for correction
            contact.setName(name.getText());
            contact.setPhone(phone.getText());
            contact.setEmail(email.getText());
            contact.setAddress(address.getText());
            saver.accept(contact);
        });
        if (saved && afterSave != null) afterSave.run();
        return saved;
    }
}
