package com.stockflow.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Read-only details popup: label/value rows plus optional extra content (e.g. a history table). */
public class DetailsDialog {
    private final Dialog<Void> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final VBox box = new VBox(14, grid);
    private int row;

    public DetailsDialog(String title) {
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(SceneNavigator.css());
        dialog.setResizable(true);
        grid.setHgap(16);
        grid.setVgap(8);
        box.setPadding(new Insets(6));
        dialog.getDialogPane().setContent(box);
    }

    public DetailsDialog row(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        Label v = new Label(Fmt.or(value, "—"));
        v.setWrapText(true);
        v.setMaxWidth(420);
        grid.add(l, 0, row);
        grid.add(v, 1, row++);
        return this;
    }

    public DetailsDialog node(Node n) {
        box.getChildren().add(n);
        return this;
    }

    public void show() {
        dialog.showAndWait();
    }
}
