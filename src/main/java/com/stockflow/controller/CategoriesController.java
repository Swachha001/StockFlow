package com.stockflow.controller;

import com.stockflow.model.Category;
import com.stockflow.service.CategoryService;
import com.stockflow.util.Alerts;
import com.stockflow.util.FormDialog;
import com.stockflow.util.TableUtil;
import com.stockflow.util.TableUtil.Action;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class CategoriesController {
    @FXML private TextField searchField;
    @FXML private CheckBox showInactive;
    @FXML private TableView<Category> table;
    @FXML private Label countLabel;

    private final CategoryService service = new CategoryService();

    @FXML
    private void initialize() {
        TableUtil.setup(table, "No categories found.");
        table.getColumns().addAll(
                TableUtil.text("Name", 220, Category::getName),
                TableUtil.text("Description", 320, Category::getDescription),
                TableUtil.number("Active Products", 120, Category::getProductCount),
                TableUtil.status("Status", 100, c -> c.isActive() ? "Active" : "Inactive"),
                TableUtil.actions(220,
                        Action.of("Edit", "btn-secondary", this::openForm),
                        new Action<Category>(c -> c.isActive() ? "Deactivate" : "Activate", "btn-danger-outline", this::toggle, c -> true)));
        searchField.textProperty().addListener((o, a, b) -> refresh());
        showInactive.selectedProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        try {
            List<Category> list = service.search(searchField.getText().trim(), showInactive.isSelected());
            table.getItems().setAll(list);
            countLabel.setText(list.size() + " categor" + (list.size() == 1 ? "y" : "ies"));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    private void toggle(Category c) {
        boolean activate = !c.isActive();
        if (!activate && !Alerts.confirm("Deactivate category", "Deactivate \"" + c.getName() + "\"?")) return;
        try {
            service.setActive(c, activate);
            refresh();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void openForm(Category existing) {
        boolean edit = existing != null;
        FormDialog d = new FormDialog(edit ? "Edit Category" : "Add Category", "Save");
        TextField name = d.text("Name *", edit ? existing.getName() : "");
        TextField desc = d.text("Description", edit ? existing.getDescription() : "");
        if (d.show(() -> {
            Category c = new Category();
            c.setId(edit ? existing.getId() : 0);
            c.setName(name.getText());
            c.setDescription(desc.getText());
            service.save(c);
        })) refresh();
    }
}
