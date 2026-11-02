package com.stockflow.controller;

import com.stockflow.model.Product;
import com.stockflow.model.Purchase;
import com.stockflow.model.PurchaseItem;
import com.stockflow.model.Supplier;
import com.stockflow.service.PurchaseService;
import com.stockflow.service.SupplierService;
import com.stockflow.util.*;
import com.stockflow.util.TableUtil.Action;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.util.List;

public class PurchasesController {
    @FXML private ComboBox<Supplier> supplierBox;
    @FXML private TextField noteField, qtyField, costField, historySearch;
    @FXML private HBox pickerHolder;
    @FXML private TableView<PurchaseItem> itemsTable;
    @FXML private Label totalLabel, historyCount;
    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<Purchase> historyTable;
    @FXML private Button confirmButton;

    private final PurchaseService service = new PurchaseService();
    private final SupplierService suppliers = new SupplierService();
    private final ObservableList<PurchaseItem> cart = FXCollections.observableArrayList();
    private ProductPicker picker;

    @FXML
    private void initialize() {
        picker = new ProductPicker();
        HBox.setHgrow(picker, Priority.ALWAYS);
        pickerHolder.getChildren().add(picker);
        picker.combo().valueProperty().addListener((o, a, p) -> {
            if (p != null) costField.setText(p.getCostPrice().toPlainString());
        });
        if (picker.getSelected() != null) costField.setText(picker.getSelected().getCostPrice().toPlainString());

        TableUtil.setup(itemsTable, "No items yet. Select a product, enter quantity and unit cost, then click \"Add Item\".");
        itemsTable.setItems(cart);
        itemsTable.getColumns().addAll(
                TableUtil.text("SKU", 120, PurchaseItem::getSku),
                TableUtil.text("Product", 260, PurchaseItem::getProductName),
                TableUtil.number("Qty", 70, PurchaseItem::getQuantity),
                TableUtil.money("Unit Cost", 120, PurchaseItem::getUnitCost),
                TableUtil.money("Subtotal", 130, PurchaseItem::getSubtotal),
                TableUtil.actions(110, Action.<PurchaseItem>of("Remove", "btn-danger-outline", i -> {
                    cart.remove(i);
                    updateTotal();
                })));

        TableUtil.setup(historyTable, "No purchases found.");
        historyTable.getColumns().addAll(
                TableUtil.text("Purchase", 100, p -> Fmt.purchaseRef(p.getId())),
                TableUtil.text("Date", 130, p -> Fmt.dateTime(p.getPurchaseDate())),
                TableUtil.text("Supplier", 220, Purchase::getSupplierName),
                TableUtil.money("Total", 130, Purchase::getTotalAmount),
                TableUtil.text("Received By", 140, Purchase::getUserName),
                TableUtil.text("Note", 200, Purchase::getNote),
                TableUtil.actions(110, Action.<Purchase>of("View", "btn-secondary", this::showItems)));
        historyTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && historyTable.getSelectionModel().getSelectedItem() != null) {
                showItems(historyTable.getSelectionModel().getSelectedItem());
            }
        });
        historySearch.textProperty().addListener((o, a, b) -> refreshHistory());
        fromDate.valueProperty().addListener((o, a, b) -> refreshHistory());
        toDate.valueProperty().addListener((o, a, b) -> refreshHistory());
        try {
            supplierBox.setItems(FXCollections.observableArrayList(suppliers.search("", false)));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
        updateTotal();
        refreshHistory();
    }

    private void updateTotal() {
        totalLabel.setText(Fmt.money(PurchaseService.total(cart)));
    }

    @FXML
    private void onAddItem() {
        try {
            Product p = picker.getSelected();
            if (p == null) throw new com.stockflow.exception.ValidationException("Select a product first.");
            int qty = Check.integer(qtyField.getText(), "Quantity", 1);
            BigDecimal cost = Check.money(costField.getText(), "Unit cost");
            PurchaseItem existing = cart.stream()
                    .filter(i -> i.getProductId() == p.getId() && i.getUnitCost().compareTo(cost) == 0).findFirst().orElse(null);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + qty);
                itemsTable.refresh();
            } else {
                PurchaseItem i = new PurchaseItem();
                i.setProductId(p.getId());
                i.setSku(p.getSku());
                i.setProductName(p.getName());
                i.setQuantity(qty);
                i.setUnitCost(cost);
                cart.add(i);
            }
            qtyField.setText("1");
            updateTotal();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    @FXML
    private void onClear() {
        if (cart.isEmpty() || Alerts.confirm("Clear items", "Remove all items from this purchase?")) {
            cart.clear();
            updateTotal();
        }
    }

    @FXML
    private void onConfirm() {
        if (supplierBox.getValue() == null) {
            Alerts.warn("Select a supplier first.");
            return;
        }
        if (cart.isEmpty()) {
            Alerts.warn("Add at least one item first.");
            return;
        }
        BigDecimal total = PurchaseService.total(cart);
        if (!Alerts.confirm("Confirm purchase", "Save purchase from " + supplierBox.getValue().getName()
                + "\nTotal: " + Fmt.money(total) + "\n\nStock will be increased.")) return;
        confirmButton.setDisable(true); // prevents a second click while saving
        try {
            int id = service.create(supplierBox.getValue(), List.copyOf(cart), noteField.getText());
            // reached only after the database transaction has committed
            cart.clear();
            noteField.clear();
            updateTotal();
            picker.reload();
            refreshHistory();
            Alerts.info("Purchase saved", Fmt.purchaseRef(id) + " saved. Stock has been updated.");
        } catch (RuntimeException e) {
            reloadLists(); // the supplier/product lists may be stale (e.g. deactivated meanwhile)
            if (e instanceof com.stockflow.exception.AppException) {
                Alerts.showFailure(e, "Purchase was NOT saved. No stock was changed.");
            } else {
                Alerts.showFailure(e);
            }
        } finally {
            confirmButton.setDisable(false);
        }
    }

    private void reloadLists() {
        try {
            Supplier selected = supplierBox.getValue();
            List<Supplier> list = suppliers.search("", false);
            supplierBox.setItems(FXCollections.observableArrayList(list));
            supplierBox.setValue(selected == null ? null
                    : list.stream().filter(x -> x.getId() == selected.getId()).findFirst().orElse(null));
            picker.reload();
        } catch (RuntimeException ignored) {
            // the original failure is what the user needs to see
        }
    }

    @FXML
    private void onClearDates() {
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    private void refreshHistory() {
        try {
            List<Purchase> list = service.search(historySearch.getText(), fromDate.getValue(), toDate.getValue());
            historyTable.getItems().setAll(list);
            historyCount.setText(list.size() + " purchase(s)  ·  Total " + Fmt.money(list.stream().map(Purchase::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void showItems(Purchase p) {
        try {
            TableView<PurchaseItem> t = new TableView<>();
            TableUtil.setup(t, "No items.");
            t.getColumns().addAll(
                    TableUtil.text("SKU", 110, PurchaseItem::getSku),
                    TableUtil.text("Product", 220, PurchaseItem::getProductName),
                    TableUtil.number("Qty", 60, PurchaseItem::getQuantity),
                    TableUtil.money("Unit Cost", 110, PurchaseItem::getUnitCost),
                    TableUtil.money("Subtotal", 120, PurchaseItem::getSubtotal));
            t.getItems().setAll(service.items(p));
            t.setPrefSize(640, 240);
            new DetailsDialog("Purchase " + Fmt.purchaseRef(p.getId()))
                    .row("Supplier", p.getSupplierName()).row("Date", Fmt.dateTime(p.getPurchaseDate()))
                    .row("Received by", p.getUserName()).row("Note", p.getNote()).row("Total", Fmt.money(p.getTotalAmount()))
                    .node(t).show();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }
}
