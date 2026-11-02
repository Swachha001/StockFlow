package com.stockflow.controller;

import com.stockflow.model.Product;
import com.stockflow.model.ReportModels.*;
import com.stockflow.service.ReportService;
import com.stockflow.service.ReportService.Range;
import com.stockflow.util.Alerts;
import com.stockflow.util.Fmt;
import com.stockflow.util.TableUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportsController {
    @FXML private ComboBox<Range> rangeBox;
    @FXML private DatePicker fromDate, toDate;
    @FXML private Label periodLabel, salesCount, salesRevenue, purchCount, purchTotal;
    @FXML private Label invProducts, invUnits, invValue, invLow, profRevenue, profCost, profProfit, profMargin;
    @FXML private TableView<TopProduct> topTable;
    @FXML private TableView<SupplierTotal> supplierTable;
    @FXML private TableView<Product> inventoryTable;

    private final ReportService service = new ReportService();

    @FXML
    private void initialize() {
        rangeBox.setItems(FXCollections.observableArrayList(Range.values()));
        rangeBox.setValue(Range.THIS_MONTH);
        rangeBox.valueProperty().addListener((o, a, r) -> {
            boolean custom = r == Range.CUSTOM;
            fromDate.setDisable(!custom);
            toDate.setDisable(!custom);
        });
        fromDate.setDisable(true);
        toDate.setDisable(true);

        TableUtil.setup(topTable, "No sales in this period.");
        topTable.getColumns().addAll(
                TableUtil.text("Product", 320, TopProduct::name),
                TableUtil.number("Units Sold", 110, TopProduct::quantity),
                TableUtil.money("Revenue", 150, TopProduct::revenue));
        TableUtil.setup(supplierTable, "No purchases in this period.");
        supplierTable.getColumns().addAll(
                TableUtil.text("Supplier", 320, SupplierTotal::supplier),
                TableUtil.number("Purchases", 110, SupplierTotal::purchases),
                TableUtil.money("Total", 150, SupplierTotal::total));
        TableUtil.setup(inventoryTable, "No products.");
        inventoryTable.getColumns().addAll(
                TableUtil.text("SKU", 120, Product::getSku),
                TableUtil.text("Product", 240, Product::getName),
                TableUtil.text("Category", 140, Product::getCategoryName),
                TableUtil.number("Stock", 70, Product::getQuantity),
                TableUtil.money("Cost", 110, Product::getCostPrice),
                TableUtil.money("Stock Value", 130, Product::getStockValue),
                TableUtil.status("Status", 130, Product::getStatusText));
        onGenerate();
    }

    @FXML
    private void onGenerate() {
        try {
            LocalDate[] r = service.resolve(rangeBox.getValue(), fromDate.getValue(), toDate.getValue());
            periodLabel.setText(Fmt.date(r[0]) + "  →  " + Fmt.date(r[1]));

            SalesSummary s = service.sales(r[0], r[1]);
            salesCount.setText(Fmt.number(s.count()));
            salesRevenue.setText(Fmt.money(s.revenue()));
            topTable.getItems().setAll(service.topProducts(r[0], r[1]));

            List<SupplierTotal> bySupplier = service.purchasesBySupplier(r[0], r[1]);
            supplierTable.getItems().setAll(bySupplier);
            purchCount.setText(Fmt.number(bySupplier.stream().mapToLong(SupplierTotal::purchases).sum()));
            purchTotal.setText(Fmt.money(bySupplier.stream().map(SupplierTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add)));

            List<Product> inv = service.inventory();
            inventoryTable.getItems().setAll(inv);
            invProducts.setText(Fmt.number(inv.size()));
            invUnits.setText(Fmt.number(inv.stream().mapToLong(Product::getQuantity).sum()));
            invValue.setText(Fmt.money(inv.stream().map(Product::getStockValue).reduce(BigDecimal.ZERO, BigDecimal::add)));
            invLow.setText(Fmt.number(inv.stream().filter(Product::isLowStock).count()));

            profRevenue.setText(Fmt.money(s.revenue()));
            profCost.setText(Fmt.money(s.cost()));
            profProfit.setText(Fmt.money(s.profit()));
            profMargin.setText(s.marginPercent().toPlainString() + " %");
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }
}
