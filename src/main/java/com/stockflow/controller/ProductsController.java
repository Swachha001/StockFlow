package com.stockflow.controller;

import com.stockflow.model.Category;
import com.stockflow.model.Permission;
import com.stockflow.model.Product;
import com.stockflow.model.Supplier;
import com.stockflow.service.CategoryService;
import com.stockflow.service.ProductService;
import com.stockflow.service.SupplierService;
import com.stockflow.util.*;
import com.stockflow.util.TableUtil.Action;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class ProductsController {
    private static final Category ALL = new Category(0, "All Categories");
    private static final Supplier NO_SUPPLIER = new Supplier(0, "— None —");

    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private CheckBox lowOnly, showInactive;
    @FXML private TableView<Product> table;
    @FXML private Label countLabel;
    @FXML private Button addButton;

    private final ProductService products = new ProductService();
    private final CategoryService categories = new CategoryService();
    private final SupplierService suppliers = new SupplierService();

    @FXML
    private void initialize() {
        TableUtil.setup(table, "No products found. Change the search/filter or add a new product.");
        boolean manage = Session.can(Permission.PRODUCTS_MANAGE);
        addButton.setVisible(manage);
        addButton.setManaged(manage);
        table.getColumns().addAll(
                TableUtil.text("SKU", 135, Product::getSku),
                TableUtil.text("Product Name", 230, Product::getName),
                TableUtil.text("Category", 140, Product::getCategoryName),
                TableUtil.text("Brand", 90, Product::getBrand),
                TableUtil.money("Cost Price", 115, Product::getCostPrice),
                TableUtil.money("Selling Price", 125, Product::getSellingPrice),
                TableUtil.number("Stock", 65, Product::getQuantity),
                TableUtil.number("Reorder", 75, Product::getReorderLevel),
                TableUtil.status("Status", 130, Product::getStatusText),
                TableUtil.actions(manage ? 235 : 90,
                        Action.of("View", "btn-secondary", this::showDetails),
                        Action.<Product>of("Edit", "btn-secondary", this::openForm, p -> manage),
                        new Action<Product>(p -> p.isActive() ? "Deactivate" : "Activate", "btn-danger-outline",
                                this::toggleActive, p -> manage)));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().removeAll("row-low", "row-inactive");
                if (!empty && p != null) {
                    if (!p.isActive()) getStyleClass().add("row-inactive");
                    else if (p.isLowStock()) getStyleClass().add("row-low");
                }
            }
        });
        try {
            List<Category> cats = new ArrayList<>(List.of(ALL));
            cats.addAll(categories.search("", false));
            categoryFilter.setItems(FXCollections.observableArrayList(cats));
            categoryFilter.setValue(ALL);
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
        searchField.textProperty().addListener((o, a, b) -> refresh());
        categoryFilter.valueProperty().addListener((o, a, b) -> refresh());
        lowOnly.selectedProperty().addListener((o, a, b) -> refresh());
        showInactive.selectedProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        try {
            Category c = categoryFilter.getValue();
            List<Product> list = products.search(searchField.getText(), c == null ? null : c.getId(), lowOnly.isSelected(), showInactive.isSelected());
            table.getItems().setAll(list);
            long low = list.stream().filter(p -> p.isActive() && p.isLowStock()).count();
            countLabel.setText(list.size() + " product(s)" + (low > 0 ? "  ·  ⚠ " + low + " low stock" : ""));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    private void showDetails(Product p) {
        new DetailsDialog("Product Details")
                .row("SKU", p.getSku()).row("Name", p.getName()).row("Category", p.getCategoryName())
                .row("Supplier", p.getSupplierName()).row("Brand", p.getBrand())
                .row("Cost price", Fmt.money(p.getCostPrice())).row("Selling price", Fmt.money(p.getSellingPrice()))
                .row("Stock", p.getQuantity() + " units").row("Reorder level", String.valueOf(p.getReorderLevel()))
                .row("Stock value (cost)", Fmt.money(p.getStockValue())).row("Status", p.getStatusText())
                .row("Created", Fmt.dateTime(p.getCreatedAt())).show();
    }

    private void toggleActive(Product p) {
        boolean activate = !p.isActive();
        if (!activate && !Alerts.confirm("Deactivate product", "Deactivate \"" + p.getName() + "\"?\nIt will no longer appear in sales and purchases.")) return;
        try {
            products.setActive(p, activate);
            refresh();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void openForm(Product existing) {
        boolean edit = existing != null;
        List<Category> cats;
        List<Supplier> sups = new ArrayList<>(List.of(NO_SUPPLIER));
        try {
            cats = new ArrayList<>(categories.search("", false));
            if (edit && cats.stream().noneMatch(c -> c.getId() == existing.getCategoryId())) {
                cats.add(new Category(existing.getCategoryId(), existing.getCategoryName()));
            }
            sups.addAll(suppliers.search("", false));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
            return;
        }
        FormDialog d = new FormDialog(edit ? "Edit Product" : "Add Product", "Save");
        TextField sku = d.text("SKU *", edit ? existing.getSku() : "");
        TextField name = d.text("Product name *", edit ? existing.getName() : "");
        ComboBox<Category> cat = d.add("Category *", new ComboBox<>(FXCollections.observableArrayList(cats)));
        ComboBox<Supplier> sup = d.add("Supplier", new ComboBox<>(FXCollections.observableArrayList(sups)));
        TextField brand = d.text("Brand", edit ? existing.getBrand() : "");
        TextField cost = d.text("Cost price (৳) *", edit ? existing.getCostPrice().toPlainString() : "");
        TextField price = d.text("Selling price (৳) *", edit ? existing.getSellingPrice().toPlainString() : "");
        TextField qty = d.text("Quantity in stock *", edit ? String.valueOf(existing.getQuantity()) : "0");
        TextField reorder = d.text("Reorder level *", edit ? String.valueOf(existing.getReorderLevel()) : "5");
        if (edit) {
            cats.stream().filter(c -> c.getId() == existing.getCategoryId()).findFirst().ifPresent(cat::setValue);
            sups.stream().filter(s -> existing.getSupplierId() != null && s.getId() == existing.getSupplierId()).findFirst().ifPresent(sup::setValue);
        }
        if (sup.getValue() == null) sup.setValue(NO_SUPPLIER);
        boolean saved = d.show(() -> {
            Product p = edit ? existing : new Product();
            Product draft = new Product();
            draft.setId(p.getId());
            draft.setSku(sku.getText());
            draft.setName(name.getText());
            draft.setCategoryId(cat.getValue() == null ? 0 : cat.getValue().getId());
            draft.setSupplierId(sup.getValue() == null || sup.getValue().getId() == 0 ? null : sup.getValue().getId());
            draft.setBrand(brand.getText());
            draft.setCostPrice(Check.money(cost.getText(), "Cost price"));
            draft.setSellingPrice(Check.money(price.getText(), "Selling price"));
            draft.setQuantity(Check.integer(qty.getText(), "Quantity", 0));
            draft.setReorderLevel(Check.integer(reorder.getText(), "Reorder level", 0));
            draft.setLoadedQuantity(edit ? existing.getQuantity() : 0);
            draft.setActive(p.isActive());
            products.save(draft);
        });
        if (saved) refresh();
    }
}
