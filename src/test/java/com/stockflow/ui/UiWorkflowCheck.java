package com.stockflow.ui;

import com.stockflow.TestDb;
import com.stockflow.dao.Jdbc;
import com.stockflow.dao.ProductDAO;
import com.stockflow.model.*;
import com.stockflow.service.*;
import com.stockflow.util.Alerts;
import com.stockflow.util.ProductPicker;
import com.stockflow.util.SceneNavigator;
import com.stockflow.util.Session;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/**
 * Headless-capable UI check (run with xvfb on Linux, see README "Testing"):
 *  1. every screen at several window sizes: no button/label may be pushed outside the window or clipped
 *  2. drives the real Purchases and Sales screens like a user (fills the form, clicks the buttons) and checks the database.
 * Uses the dedicated *_test database only. Usage: UiWorkflowCheck <screenshot-dir>
 */
public class UiWorkflowCheck {
    static int failures;
    static final List<String> dialogs = Collections.synchronizedList(new ArrayList<>());
    static String shots;

    static void check(String name, boolean ok) {
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
    }

    static void pulse() throws Exception {
        for (int i = 0; i < 3; i++) {
            CountDownLatch l = new CountDownLatch(1);
            Platform.runLater(l::countDown);
            l.await();
            Thread.sleep(60);
        }
    }

    static <T> T onFx(Callable<T> c) throws Exception {
        FutureTask<T> f = new FutureTask<>(c);
        Platform.runLater(f);
        return f.get();
    }

    static void all(Node n, List<Node> out) {
        out.add(n);
        if (n instanceof TableView) return;
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) all(c, out);
    }

    static Button button(Parent root, String textPart) {
        List<Node> nodes = new ArrayList<>();
        all(root, nodes);
        for (Node n : nodes) if (n instanceof Button b && b.getText() != null && b.getText().contains(textPart)) return b;
        throw new NoSuchElementException("button " + textPart);
    }

    static class Ui {
        Stage stage;
        Parent root;
        Node content;
    }

    static Ui open(String user, String pw, int w, int h, String navText) throws Exception {
        Ui ui = new Ui();
        onFx(() -> {
            new AuthService().logout();
            new AuthService().login(user, pw);
            ui.root = SceneNavigator.load("MainView");
            Scene sc = new Scene(ui.root, w, h);
            sc.getStylesheets().add(SceneNavigator.css());
            ui.stage = new Stage();
            ui.stage.setScene(sc);
            ui.stage.show();
            return null;
        });
        pulse();
        if (navText != null) {
            onFx(() -> {
                List<Node> nodes = new ArrayList<>();
                all(ui.root, nodes);
                for (Node n : nodes) if (n instanceof Button b && b.getStyleClass().contains("nav-button") && b.getText().contains(navText)) { b.fire(); break; }
                return null;
            });
            pulse();
        }
        ui.content = onFx(() -> ui.root.lookup("#content"));
        return ui;
    }

    static void close(Ui ui) throws Exception {
        onFx(() -> { ui.stage.close(); return null; });
    }

    static void screenshot(Ui ui, String name) throws Exception {
        if (shots == null) return;
        onFx(() -> {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(ui.root.getScene().snapshot(null), null), "png", new File(shots, name + ".png"));
            } catch (Exception e) { e.printStackTrace(); }
            return null;
        });
    }

    static List<String> clipped(Ui ui, int w, int h) throws Exception {
        return onFx(() -> {
            List<String> out = new ArrayList<>();
            Scene sc = ui.root.getScene();
            List<Node> nodes = new ArrayList<>();
            all(ui.root, nodes);
            for (Node n : nodes) {
                boolean interesting = n instanceof Button || (n instanceof Label l && (l.getStyleClass().contains("muted") || l.getStyleClass().contains("total-value")));
                if (!interesting || !n.isVisible() || !n.isManaged() || n.getStyleClass().contains("nav-button")) continue;
                boolean inContent = false;
                for (Node p = n; p != null; p = p.getParent()) if (p == ui.content) inContent = true;
                if (!inContent) continue;
                var nb = n.localToScene(n.getBoundsInLocal());
                String why = null;
                if (nb.getMaxY() > sc.getHeight() + 1 || nb.getMaxX() > sc.getWidth() + 1) why = "outside window";
                for (Node p = n.getParent(); p != null && why == null; p = p.getParent()) {
                    if (p instanceof Region r && !(p instanceof ScrollPane)) {
                        var pb = r.localToScene(r.getLayoutBounds());
                        if (nb.getMaxY() > pb.getMaxY() + 1 || nb.getMaxX() > pb.getMaxX() + 1 || nb.getMinY() < pb.getMinY() - 1) why = "clipped by " + p.getClass().getSimpleName();
                    }
                }
                if (why != null) out.add((n instanceof Labeled lb ? lb.getText() : n.getClass().getSimpleName()) + " (" + why + ")");
            }
            return out;
        });
    }

    static long count(String sql) { return Jdbc.one(sql, rs -> rs.getLong(1)); }

    static String lastDialog() { synchronized (dialogs) { return dialogs.isEmpty() ? "" : dialogs.get(dialogs.size() - 1); } }

    public static void main(String[] args) throws Exception {
        shots = args.length > 0 ? args[0] : null;
        if (shots != null) new File(shots).mkdirs();
        if (!TestDb.available()) { System.out.println("SKIPPED: test database not reachable"); return; }
        TestDb.rebuild();
        Platform.startup(() -> {});
        Platform.setImplicitExit(false);
        SceneNavigator.init(onFx(Stage::new));
        Alerts.setTestSink(dialogs);

        // --- test data via services
        AuthService auth = new AuthService();
        auth.login("admin", TestDb.ADMIN_PW);
        Category cat = new Category(); cat.setName("UI Cat"); new CategoryService().save(cat);
        Supplier sup = new Supplier(); sup.setName("UI Supplier"); sup.setPhone("01712345678"); new SupplierService().save(sup);
        Product p = new Product();
        p.setSku("UI-100"); p.setName("UI Widget"); p.setCategoryId(cat.getId());
        p.setCostPrice(new BigDecimal("100")); p.setSellingPrice(new BigDecimal("150")); p.setQuantity(10); p.setReorderLevel(2);
        new ProductService().save(p);
        auth.logout();
        ProductDAO dao = new ProductDAO();

        System.out.println("== 1. layout: nothing may be pushed out of / clipped by the window ==");
        String[] screens = {"Dashboard", "Products", "Categories", "Purchases", "Sales", "Suppliers", "Customers", "Reports", "Users", "Activity Logs", "Settings"};
        for (int[] sz : new int[][]{{1000, 640}, {1280, 780}, {1000, 560}}) {
            for (String sc : screens) {
                Ui ui = open("admin", TestDb.ADMIN_PW, sz[0], sz[1], sc);
                List<String> bad = clipped(ui, sz[0], sz[1]);
                check("admin  " + sc + " @" + sz[0] + "x" + sz[1] + (bad.isEmpty() ? "" : "  -> " + bad), bad.isEmpty());
                if (sc.equals("Purchases") || sc.equals("Sales")) screenshot(ui, sc + "_" + sz[0] + "x" + sz[1]);
                close(ui);
            }
        }
        for (String[] u : new String[][]{{"manager", TestDb.MANAGER_PW, "Purchases"}, {"staff", TestDb.STAFF_PW, "Sales"}}) {
            Ui ui = open(u[0], u[1], 1000, 640, u[2]);
            List<String> bad = clipped(ui, 1000, 640);
            check(u[0] + " " + u[2] + " @1000x640" + (bad.isEmpty() ? "" : "  -> " + bad), bad.isEmpty());
            close(ui);
        }
        {   // the admin sidebar is taller than a 640px window: it must scroll instead of pushing the page out
            Ui ui = open("admin", TestDb.ADMIN_PW, 1000, 560, "Purchases");
            boolean ok = onFx(() -> {
                Button logout = button(ui.root, "Logout");
                var b = logout.localToScene(logout.getBoundsInLocal());
                return b.getMaxY() <= ui.root.getScene().getHeight() + 1;
            });
            check("Logout button stays visible in a short window (sidebar menu scrolls)", ok);
            close(ui);
        }

        System.out.println("== 2. Purchases screen driven like a user ==");
        Ui ui = open("admin", TestDb.ADMIN_PW, 1000, 640, "Purchases");
        @SuppressWarnings("unchecked") ComboBox<Supplier> supplierBox = (ComboBox<Supplier>) onFx(() -> ui.root.lookup("#supplierBox"));
        TextField qty = (TextField) onFx(() -> ui.root.lookup("#qtyField"));
        TextField cost = (TextField) onFx(() -> ui.root.lookup("#costField"));
        @SuppressWarnings("unchecked") TableView<PurchaseItem> itemsTable = (TableView<PurchaseItem>) onFx(() -> ui.root.lookup("#itemsTable"));
        @SuppressWarnings("unchecked") TableView<Purchase> history = (TableView<Purchase>) onFx(() -> ui.root.lookup("#historyTable"));
        Label total = (Label) onFx(() -> ui.root.lookup("#totalLabel"));
        Button confirm = (Button) onFx(() -> ui.root.lookup("#confirmButton"));
        ProductPicker picker = onFx(() -> (ProductPicker) ((HBox) ui.root.lookup("#pickerHolder")).getChildren().get(0));
        check("Confirm Purchase button exists and is enabled", !confirm.isDisabled());

        onFx(() -> {
            supplierBox.setValue(supplierBox.getItems().stream().filter(s -> s.getName().equals("UI Supplier")).findFirst().orElseThrow());
            picker.searchField().setText("UI-100");
            qty.setText("5");
            cost.setText("95.00");
            button(ui.root, "Add Item").fire();
            return null;
        });
        check("item added to cart (1 line)", onFx(() -> itemsTable.getItems().size()) == 1);
        check("cart total shows ৳ 475.00", onFx(() -> total.getText()).equals("৳ 475.00"));
        screenshot(ui, "Purchase_cart_filled_1000x640");
        long purchasesBefore = count("SELECT COUNT(*) FROM purchases");
        dialogs.clear();
        onFx(() -> { confirm.fire(); return null; });
        check("success dialog shown after save: " + lastDialog().replace("\n", " "), lastDialog().startsWith("INFO: Purchase saved"));
        check("purchase row written (+1)", count("SELECT COUNT(*) FROM purchases") == purchasesBefore + 1);
        check("purchase item row written with qty 5 / cost 95.00", count("SELECT COUNT(*) FROM purchase_items WHERE quantity=5 AND unit_cost=95.00") == 1);
        check("stock 10 -> 15 in database", dao.findById(p.getId()).getQuantity() == 15);
        check("cart cleared after save", onFx(() -> itemsTable.getItems().isEmpty()));
        check("history tab lists the new purchase", onFx(() -> history.getItems().stream().anyMatch(x -> x.getSupplierName().equals("UI Supplier")
                && x.getTotalAmount().compareTo(new BigDecimal("475.00")) == 0)));

        dialogs.clear();
        onFx(() -> { confirm.fire(); return null; });
        check("confirm with empty cart is refused: " + lastDialog(), lastDialog().startsWith("WARN: Add at least one item"));
        check("... and nothing changed", dao.findById(p.getId()).getQuantity() == 15 && count("SELECT COUNT(*) FROM purchases") == purchasesBefore + 1);

        dialogs.clear();
        onFx(() -> { qty.setText("2000000"); button(ui.root, "Add Item").fire(); confirm.fire(); return null; });
        check("oversized quantity refused with clear message: " + lastDialog().replace("\n", " "), lastDialog().contains("Purchase was NOT saved") && lastDialog().contains("too large"));
        check("... no purchase row, stock still 15, cart kept for correction", count("SELECT COUNT(*) FROM purchases") == purchasesBefore + 1
                && dao.findById(p.getId()).getQuantity() == 15 && onFx(() -> itemsTable.getItems().size()) == 1);

        dialogs.clear();
        onFx(() -> {
            itemsTable.getItems().clear();
            qty.setText("3"); cost.setText("90");
            button(ui.root, "Add Item").fire();
            return null;
        });
        Product live = dao.findById(p.getId());
        auth.login("admin", TestDb.ADMIN_PW);
        new ProductService().setActive(live, false);     // someone deactivates the product while the cart is open
        dialogs.clear();
        onFx(() -> { confirm.fire(); return null; });
        check("deactivated-meanwhile product refused: " + lastDialog().replace("\n", " "), lastDialog().contains("NOT saved") && lastDialog().contains("no longer active"));
        check("... stock unchanged (15), no purchase row", dao.findById(p.getId()).getQuantity() == 15 && count("SELECT COUNT(*) FROM purchases") == purchasesBefore + 1);
        check("... Confirm button usable again (not left disabled)", !onFx(confirm::isDisabled));
        new ProductService().setActive(dao.findById(p.getId()), true);
        screenshot(ui, "Purchase_after_error_1000x640");
        close(ui);

        System.out.println("== 3. Sales screen driven like a user ==");
        Ui sui = open("staff", TestDb.STAFF_PW, 1000, 640, "Sales");
        TextField sqty = (TextField) onFx(() -> sui.root.lookup("#qtyField"));
        @SuppressWarnings("unchecked") TableView<SaleItem> cart = (TableView<SaleItem>) onFx(() -> sui.root.lookup("#cartTable"));
        Button complete = (Button) onFx(() -> sui.root.lookup("#completeButton"));
        ProductPicker spicker = onFx(() -> (ProductPicker) ((HBox) sui.root.lookup("#pickerHolder")).getChildren().get(0));
        long salesBefore = count("SELECT COUNT(*) FROM sales");
        onFx(() -> { spicker.searchField().setText("UI-100"); sqty.setText("4"); button(sui.root, "Add to Cart").fire(); return null; });
        check("sale line added", onFx(() -> cart.getItems().size()) == 1);
        dialogs.clear();
        onFx(() -> { complete.fire(); return null; });
        check("sale completed dialog: " + lastDialog().replace("\n", " ").substring(0, Math.min(60, lastDialog().length())), lastDialog().startsWith("INFO: Sale completed"));
        check("stock 15 -> 11 and one sale row", dao.findById(p.getId()).getQuantity() == 11 && count("SELECT COUNT(*) FROM sales") == salesBefore + 1);
        dialogs.clear();
        onFx(() -> { sqty.setText("50"); button(sui.root, "Add to Cart").fire(); return null; });
        check("exceeding stock is refused (Available/Requested): " + lastDialog().replace("\n", " "), lastDialog().contains("Available: 11") && lastDialog().contains("Requested: 50"));
        check("... cart empty, stock still 11", onFx(() -> cart.getItems().isEmpty()) && dao.findById(p.getId()).getQuantity() == 11);
        close(sui);

        auth.logout();
        Alerts.setTestSink(null);
        TestDb.dropAllTables();
        System.out.println("\nUI CHECK RESULT: " + failures + " failure(s)");
        Platform.exit();
        System.exit(failures == 0 ? 0 : 1);
    }
}
