package com.stockflow.controller;

import com.stockflow.exception.ValidationException;
import com.stockflow.model.*;
import com.stockflow.service.CustomerService;
import com.stockflow.service.SaleService;
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
import java.util.ArrayList;
import java.util.List;

public class SalesController {
    private static final Customer WALK_IN = new Customer(0, "Walk-in Customer");

    @FXML private ComboBox<Customer> customerBox;
    @FXML private Button newCustomerButton;
    @FXML private HBox pickerHolder;
    @FXML private TextField qtyField, historySearch;
    @FXML private TableView<SaleItem> cartTable;
    @FXML private Label totalLabel, historyCount;
    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<Sale> historyTable;

    private final SaleService service = new SaleService();
    private final CustomerService customers = new CustomerService();
    private final ObservableList<SaleItem> cart = FXCollections.observableArrayList();
    private ProductPicker picker;

    @FXML
    private void initialize() {
        picker = new ProductPicker();
        HBox.setHgrow(picker, Priority.ALWAYS);
        pickerHolder.getChildren().add(picker);
        picker.searchField().setOnAction(e -> onAddItem());
        boolean canAddCustomer = Session.can(Permission.CUSTOMERS_ADD);
        newCustomerButton.setVisible(canAddCustomer);
        newCustomerButton.setManaged(canAddCustomer);

        TableUtil.setup(cartTable, "Cart is empty. Search a product and click \"Add to Cart\".");
        cartTable.setItems(cart);
        cartTable.getColumns().addAll(
                TableUtil.text("SKU", 120, SaleItem::getSku),
                TableUtil.text("Product", 260, SaleItem::getProductName),
                TableUtil.number("Qty", 70, SaleItem::getQuantity),
                TableUtil.money("Unit Price", 120, SaleItem::getUnitPrice),
                TableUtil.money("Subtotal", 130, SaleItem::getSubtotal),
                TableUtil.actions(110, Action.<SaleItem>of("Remove", "btn-danger-outline", i -> {
                    cart.remove(i);
                    updateTotal();
                })));

        TableUtil.setup(historyTable, "No sales found.");
        historyTable.getColumns().addAll(
                TableUtil.text("Invoice", 110, Sale::getInvoiceNo),
                TableUtil.text("Date", 130, s -> Fmt.dateTime(s.getSaleDate())),
                TableUtil.text("Customer", 220, Sale::getCustomerName),
                TableUtil.money("Total", 130, Sale::getTotalAmount),
                TableUtil.text("Cashier", 150, Sale::getUserName),
                TableUtil.actions(110, Action.<Sale>of("View", "btn-secondary", this::showItems)));
        historyTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && historyTable.getSelectionModel().getSelectedItem() != null) {
                showItems(historyTable.getSelectionModel().getSelectedItem());
            }
        });
        historySearch.textProperty().addListener((o, a, b) -> refreshHistory());
        fromDate.valueProperty().addListener((o, a, b) -> refreshHistory());
        toDate.valueProperty().addListener((o, a, b) -> refreshHistory());
        loadCustomers(null);
        updateTotal();
        refreshHistory();
    }

    private void loadCustomers(Integer selectId) {
        try {
            List<Customer> list = new ArrayList<>(List.of(WALK_IN));
            list.addAll(customers.search("", false));
            customerBox.setItems(FXCollections.observableArrayList(list));
            Customer sel = selectId == null ? WALK_IN
                    : list.stream().filter(c -> c.getId() == selectId).findFirst().orElse(WALK_IN);
            customerBox.setValue(sel);
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void updateTotal() {
        totalLabel.setText(Fmt.money(SaleService.total(cart)));
    }

    @FXML
    private void onNewCustomer() {
        Customer c = new Customer();
        if (ContactController.openForm("Customer", c, customers::save, null)) loadCustomers(c.getId());
    }

    @FXML
    private void onAddItem() {
        try {
            Product p = picker.getSelected();
            if (p == null) throw new ValidationException("Select a product first.");
            int qty = Check.integer(qtyField.getText(), "Quantity", 1);
            SaleItem existing = cart.stream().filter(i -> i.getProductId() == p.getId()).findFirst().orElse(null);
            int wanted = qty + (existing == null ? 0 : existing.getQuantity());
            if (wanted > p.getQuantity()) {
                Alerts.warn("Insufficient stock for " + p.getName() + ".\n\nAvailable: " + p.getQuantity()
                        + "\nRequested: " + wanted + (existing == null ? "" : " (including " + existing.getQuantity() + " already in cart)"));
                return;
            }
            if (existing != null) {
                existing.setQuantity(wanted);
                cartTable.refresh();
            } else {
                SaleItem i = new SaleItem();
                i.setProductId(p.getId());
                i.setSku(p.getSku());
                i.setProductName(p.getName());
                i.setQuantity(qty);
                i.setUnitPrice(p.getSellingPrice());
                i.setUnitCost(p.getCostPrice());
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
        if (cart.isEmpty() || Alerts.confirm("Clear cart", "Remove all items from the cart?")) {
            cart.clear();
            updateTotal();
        }
    }

    @FXML
    private void onComplete() {
        try {
            if (cart.isEmpty()) throw new ValidationException("The cart is empty.");
            Customer c = customerBox.getValue();
            if (!Alerts.confirm("Complete sale", "Customer: " + (c == null ? WALK_IN : c).getName()
                    + "\nTotal: " + Fmt.money(SaleService.total(cart)) + "\n\nStock will be decreased.")) return;
            Sale sale = service.create(c, List.copyOf(cart));
            StringBuilder receipt = new StringBuilder();
            receipt.append("Invoice: ").append(sale.getInvoiceNo()).append("\nCustomer: ").append(sale.getCustomerName()).append("\n\n");
            for (SaleItem i : sale.getItems()) {
                receipt.append(i.getQuantity()).append(" x ").append(i.getProductName()).append("  —  ").append(Fmt.money(i.getSubtotal())).append('\n');
            }
            receipt.append("\nTOTAL: ").append(Fmt.money(sale.getTotalAmount()));
            cart.clear();
            updateTotal();
            picker.reload();
            refreshHistory();
            Alerts.info("Sale completed", receipt.toString());
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
            picker.reload(); // stock may have changed
        }
    }

    @FXML
    private void onClearDates() {
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    private void refreshHistory() {
        try {
            List<Sale> list = service.search(historySearch.getText(), fromDate.getValue(), toDate.getValue());
            historyTable.getItems().setAll(list);
            historyCount.setText(list.size() + " sale(s)  ·  Total " + Fmt.money(list.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private void showItems(Sale s) {
        try {
            TableView<SaleItem> t = new TableView<>();
            TableUtil.setup(t, "No items.");
            t.getColumns().addAll(
                    TableUtil.text("SKU", 110, SaleItem::getSku),
                    TableUtil.text("Product", 220, SaleItem::getProductName),
                    TableUtil.number("Qty", 60, SaleItem::getQuantity),
                    TableUtil.money("Unit Price", 110, SaleItem::getUnitPrice),
                    TableUtil.money("Subtotal", 120, SaleItem::getSubtotal));
            t.getItems().setAll(service.items(s));
            t.setPrefSize(640, 240);
            new DetailsDialog("Invoice " + s.getInvoiceNo())
                    .row("Customer", s.getCustomerName()).row("Date", Fmt.dateTime(s.getSaleDate()))
                    .row("Cashier", s.getUserName()).row("Total", Fmt.money(s.getTotalAmount()))
                    .node(t).show();
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }
}
