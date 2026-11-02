package com.stockflow.util;

import com.stockflow.exception.AppException;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Simple form popup. The save action runs inside the dialog: errors are shown inline and the dialog stays open. */
public class FormDialog {
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final Label error = new Label();
    private final ButtonType saveType;
    private int row;

    public FormDialog(String title, String saveText) {
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        saveType = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(SceneNavigator.css());
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 4, 4, 4));
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setMaxWidth(420);
        error.setVisible(false);
        error.setManaged(false);
        dialog.getDialogPane().setContent(new VBox(10, grid, error));
    }

    public <T extends Node> T add(String label, T node) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        grid.add(l, 0, row);
        grid.add(node, 1, row++);
        GridPane.setHgrow(node, Priority.ALWAYS);
        if (node instanceof Region r) r.setPrefWidth(300);
        return node;
    }

    public TextField text(String label, String value) {
        TextField f = new TextField(value == null ? "" : value);
        return add(label, f);
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
    }

    /** Shows the dialog; returns true if saved successfully. */
    public boolean show(Runnable onSave) {
        Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
        save.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                onSave.run();
            } catch (com.stockflow.exception.SessionExpiredException ex) {
                e.consume();
                dialog.close();
                javafx.application.Platform.runLater(() -> Alerts.showFailure(ex));
            } catch (AppException ex) {
                showError(ex.getMessage());
                e.consume();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                showError("Unexpected error: " + ex.getMessage());
                e.consume();
            }
        });
        return dialog.showAndWait().filter(b -> b == saveType).isPresent();
    }
}
