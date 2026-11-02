package com.stockflow.util;

import com.stockflow.model.Product;
import com.stockflow.service.ProductService;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.List;

/** Search box + dropdown of active products (shows current stock). */
public class ProductPicker extends HBox {
    private final TextField search = new TextField();
    private final ComboBox<Product> combo = new ComboBox<>();
    private final ProductService service = new ProductService();

    public ProductPicker() {
        super(8);
        search.setPromptText("Search SKU / name / brand");
        search.setPrefWidth(210);
        search.setMinWidth(120);
        combo.setPromptText("Select product");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setMinWidth(160);
        combo.setVisibleRowCount(10);
        HBox.setHgrow(combo, Priority.ALWAYS);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(Product p) {
                return p == null ? "" : p.getSku() + " — " + p.getName() + "  (Stock: " + p.getQuantity() + ")";
            }

            @Override public Product fromString(String s) { return null; }
        });
        search.textProperty().addListener((o, a, b) -> reload());
        getChildren().addAll(search, combo);
        reload();
    }

    public void reload() {
        Product previous = combo.getValue();
        List<Product> list = service.searchActive(search.getText());
        combo.setItems(FXCollections.observableArrayList(list));
        Product match = previous == null ? null
                : list.stream().filter(p -> p.getId() == previous.getId()).findFirst().orElse(null);
        combo.setValue(match != null ? match : (list.isEmpty() ? null : list.get(0)));
    }

    public Product getSelected() { return combo.getValue(); }

    public ComboBox<Product> combo() { return combo; }

    public TextField searchField() { return search; }
}
