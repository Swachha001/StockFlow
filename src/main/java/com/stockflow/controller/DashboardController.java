package com.stockflow.controller;

import com.stockflow.model.Product;
import com.stockflow.model.ReportModels.*;
import com.stockflow.service.DashboardService;
import com.stockflow.util.Alerts;
import com.stockflow.util.Fmt;
import com.stockflow.util.TableUtil;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class DashboardController {
    @FXML private FlowPane cards;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private TableView<Product> lowStockTable;
    @FXML private TableView<Txn> recentTable;

    private final DashboardService service = new DashboardService();

    @FXML
    private void initialize() {
        TableUtil.setup(lowStockTable, "✔ All products are sufficiently stocked.");
        lowStockTable.getColumns().addAll(
                TableUtil.text("SKU", 110, Product::getSku),
                TableUtil.text("Product", 200, Product::getName),
                TableUtil.number("Stock", 60, Product::getQuantity),
                TableUtil.number("Reorder", 70, Product::getReorderLevel),
                TableUtil.status("Status", 120, Product::getStatusText));
        TableUtil.setup(recentTable, "No transactions yet.");
        recentTable.getColumns().addAll(
                TableUtil.text("Type", 90, Txn::type),
                TableUtil.text("Reference", 120, Txn::ref),
                TableUtil.text("Customer / Supplier", 240, Txn::party),
                TableUtil.money("Amount", 140, Txn::amount),
                TableUtil.text("Date", 140, t -> Fmt.dateTime(t.when())));
        refresh();
    }

    @FXML
    private void refresh() {
        try {
            DashboardStats s = service.stats();
            cards.getChildren().setAll(
                    card("Total Products", Fmt.number(s.totalProducts()), "card-blue"),
                    card("Total Stock Units", Fmt.number(s.totalUnits()), "card-blue"),
                    card(s.lowStock() > 0 ? "⚠ Low Stock Items" : "Low Stock Items", Fmt.number(s.lowStock()), s.lowStock() > 0 ? "card-amber" : "card-green"),
                    card("Stock Value (at cost)", Fmt.money(s.stockValue()), "card-blue"),
                    card("Today's Sales (invoices)", Fmt.number(s.todaySalesCount()), "card-green"),
                    card("Today's Revenue", Fmt.money(s.todayRevenue()), "card-green"),
                    card("Today's Purchases", Fmt.money(s.todayPurchases()), "card-blue"),
                    card("Est. Profit (today)", Fmt.money(s.todayProfit()), "card-green"));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd-MM");
            for (DailySales d : service.last7Days()) {
                series.getData().add(new XYChart.Data<>(dm.format(d.date()), d.revenue()));
            }
            salesChart.getData().setAll(series);
            lowStockTable.getItems().setAll(service.lowStock());
            recentTable.getItems().setAll(service.recent());
        } catch (RuntimeException e) {
            Alerts.showFailure(e);
        }
    }

    private VBox card(String title, String value, String style) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        Label v = new Label(value);
        v.getStyleClass().add("card-value");
        VBox box = new VBox(6, t, v);
        box.getStyleClass().addAll("card", style);
        box.setPrefWidth(236);
        return box;
    }
}
