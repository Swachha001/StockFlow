package com.stockflow.util;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TableUtil {
    private TableUtil() { }

    /** A per-row button shown in an "Actions" column. */
    public record Action<S>(Function<S, String> label, String styleClass, Consumer<S> handler, Predicate<S> visible) {
        public static <S> Action<S> of(String label, String style, Consumer<S> handler, Predicate<S> visible) {
            return new Action<>(s -> label, style, handler, visible);
        }

        public static <S> Action<S> of(String label, String style, Consumer<S> handler) {
            return new Action<>(s -> label, style, handler, s -> true);
        }
    }

    public static <S> void setup(TableView<S> table, String emptyMessage) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // JavaFX tables default to a 400px minimum height, which pushes the rows below them out of small windows.
        table.setMinHeight(120);
        Label placeholder = new Label(emptyMessage);
        placeholder.getStyleClass().add("empty-state");
        table.setPlaceholder(placeholder);
    }

    public static <S, T> TableColumn<S, T> col(String title, double width, Function<S, T> getter) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setMinWidth(width * 0.75);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(getter.apply(cd.getValue())));
        return c;
    }

    public static <S> TableColumn<S, String> text(String title, double width, Function<S, String> getter) {
        TableColumn<S, String> c = col(title, width, s -> {
            String v = getter.apply(s);
            return v == null ? "" : v;
        });
        c.setStyle("-fx-alignment: CENTER-LEFT;");
        return c;
    }

    public static <S> TableColumn<S, String> money(String title, double width, Function<S, BigDecimal> getter) {
        TableColumn<S, String> c = text(title, width, s -> Fmt.money(getter.apply(s)));
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    public static <S> TableColumn<S, String> number(String title, double width, Function<S, Number> getter) {
        TableColumn<S, String> c = text(title, width, s -> Fmt.number(getter.apply(s).longValue()));
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    /** Text with a coloured badge look. ⚠ = warning, ✖ = danger, Inactive = muted, anything else = OK. */
    public static <S> TableColumn<S, String> status(String title, double width, Function<S, String> getter) {
        TableColumn<S, String> c = text(title, width, getter);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(item.startsWith("⚠") ? "badge-warn"
                        : item.startsWith("✖") ? "badge-danger"
                        : item.equals("Inactive") ? "badge-muted" : "badge-ok");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        return c;
    }

    @SafeVarargs
    public static <S> TableColumn<S, Void> actions(double width, Action<S>... actions) {
        TableColumn<S, Void> c = new TableColumn<>("Actions");
        c.setPrefWidth(width);
        c.setMinWidth(width);
        c.setSortable(false);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                @SuppressWarnings("unchecked")
                S row = (S) getTableRow().getItem();
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER_LEFT);
                for (Action<S> a : actions) {
                    if (!a.visible().test(row)) continue;
                    Button b = new Button(a.label().apply(row));
                    b.getStyleClass().addAll("btn-small", a.styleClass());
                    b.setOnAction(e -> a.handler().accept(row));
                    box.getChildren().add(b);
                }
                setGraphic(box);
            }
        });
        return c;
    }
}
