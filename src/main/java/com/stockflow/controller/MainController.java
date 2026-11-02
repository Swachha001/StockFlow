package com.stockflow.controller;

import com.stockflow.exception.SessionExpiredException;
import com.stockflow.model.Permission;
import com.stockflow.service.AuthService;
import com.stockflow.util.Alerts;
import com.stockflow.util.Fmt;
import com.stockflow.util.SceneNavigator;
import com.stockflow.util.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MainController {
    @FXML private VBox sidebar;
    @FXML private StackPane content;
    @FXML private Label welcomeLabel, dateLabel, userLabel;

    private record NavItem(String label, String icon, Permission permission, Supplier<Node> view) { }

    private record Group(String title, List<NavItem> items) { }

    private final List<Button> navButtons = new ArrayList<>();
    private final AuthService auth = new AuthService();
    private Timeline sessionWatch;

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome, " + Session.user().getFullName());
        userLabel.setText(Session.user().getUsername() + " · " + Session.user().getRole());
        dateLabel.setText(Fmt.date(LocalDate.now()));
        buildSidebar();
        startSessionWatch();
    }

    /** Every 30 s: if this account was deactivated or its role changed, send the user back to the login screen. */
    private void startSessionWatch() {
        sessionWatch = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try {
                Session.revalidate();
            } catch (SessionExpiredException ex) {
                sessionWatch.stop();
                Alerts.showFailure(ex);
            } catch (RuntimeException ignored) {
                // database temporarily unreachable: the next real action will report it
            }
        }));
        sessionWatch.setCycleCount(Timeline.INDEFINITE);
        sessionWatch.play();
    }

    private List<Group> groups() {
        return List.of(
                new Group(null, List.of(new NavItem("Dashboard", "▦", Permission.DASHBOARD, () -> SceneNavigator.load("DashboardView")))),
                new Group("INVENTORY", List.of(
                        new NavItem("Products", "▣", Permission.PRODUCTS_VIEW, () -> SceneNavigator.load("ProductsView")),
                        new NavItem("Categories", "☰", Permission.CATEGORIES, () -> SceneNavigator.load("CategoriesView")))),
                new Group("TRANSACTIONS", List.of(
                        new NavItem("Purchases", "⇩", Permission.PURCHASES, () -> SceneNavigator.load("PurchasesView")),
                        new NavItem("Sales", "⇧", Permission.SALES, () -> SceneNavigator.load("SalesView")))),
                new Group("CONTACTS", List.of(
                        new NavItem("Suppliers", "⚑", Permission.SUPPLIERS, () -> SceneNavigator.load("ContactView", new SuppliersController())),
                        new NavItem("Customers", "☺", Permission.CUSTOMERS_VIEW, () -> SceneNavigator.load("ContactView", new CustomersController())))),
                new Group("INSIGHTS", List.of(new NavItem("Reports", "▤", Permission.REPORTS, () -> SceneNavigator.load("ReportsView")))),
                new Group("ADMINISTRATION", List.of(
                        new NavItem("Users", "☻", Permission.USERS, () -> SceneNavigator.load("UsersView")),
                        new NavItem("Activity Logs", "≣", Permission.LOGS, () -> SceneNavigator.load("ActivityLogsView")))),
                new Group("SYSTEM", List.of(new NavItem("Settings", "⚙", null, () -> SceneNavigator.load("SettingsView")))));
    }

    private void buildSidebar() {
        Label brand = new Label("STOCKFLOW");
        brand.getStyleClass().add("sidebar-brand");
        Label sub = new Label("Retail Inventory");
        sub.getStyleClass().add("sidebar-sub");
        sidebar.getChildren().addAll(brand, sub);

        // The menu scrolls if the window is shorter than the menu; otherwise an un-shrinkable sidebar would force the
        // whole window content (including the Confirm/Complete buttons at the bottom of a page) off the screen.
        VBox nav = new VBox(2);
        ScrollPane navScroll = new ScrollPane(nav);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.getStyleClass().add("nav-scroll");
        nav.getStyleClass().add("nav-box");
        VBox.setVgrow(navScroll, Priority.ALWAYS);
        navScroll.setMinHeight(80);

        NavItem first = null;
        Button firstButton = null;
        for (Group g : groups()) {
            List<NavItem> allowed = g.items().stream()
                    .filter(i -> i.permission() == null || Session.can(i.permission())).toList();
            if (allowed.isEmpty()) continue;
            if (g.title() != null) {
                Label section = new Label(g.title());
                section.getStyleClass().add("sidebar-section");
                nav.getChildren().add(section);
            }
            for (NavItem item : allowed) {
                Button b = new Button(item.icon() + "   " + item.label());
                b.getStyleClass().add("nav-button");
                b.setMaxWidth(Double.MAX_VALUE);
                b.setOnAction(e -> open(item, b));
                navButtons.add(b);
                nav.getChildren().add(b);
                if (first == null) {
                    first = item;
                    firstButton = b;
                }
            }
        }

        Button logout = new Button("⏻   Logout");
        logout.getStyleClass().addAll("nav-button", "nav-logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> logout());
        sidebar.getChildren().addAll(navScroll, logout);

        if (first != null) open(first, firstButton);
    }

    private void open(NavItem item, Button button) {
        navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
        button.getStyleClass().add("nav-active");
        try {
            content.getChildren().setAll(item.view().get());
        } catch (RuntimeException e) {
            e.printStackTrace();
            Label err = new Label("Could not open " + item.label() + ".\n" + e.getMessage());
            err.getStyleClass().add("form-error");
            content.getChildren().setAll(err);
        }
    }

    private void logout() {
        if (!Alerts.confirm("Logout", "Do you want to log out?")) return;
        if (sessionWatch != null) sessionWatch.stop();
        auth.logout();
        SceneNavigator.showLogin();
    }
}
