package com.stockflow.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class Alerts {
    /** Test seam: when set, dialogs are not shown; their text is recorded here and confirmations answer "yes". */
    private static volatile java.util.List<String> testSink;

    private Alerts() { }

    public static void setTestSink(java.util.List<String> sink) { testSink = sink; }

    private static boolean recorded(String kind, String message) {
        java.util.List<String> sink = testSink;
        if (sink == null) return false;
        sink.add(kind + ": " + message);
        return true;
    }

    private static Alert make(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(SceneNavigator.css());
        a.getDialogPane().setMinWidth(380);
        return a;
    }

    public static void info(String title, String message) { if (recorded("INFO", title + " | " + message)) return; make(Alert.AlertType.INFORMATION, title, message).showAndWait(); }

    public static void warn(String message) { if (recorded("WARN", message)) return; make(Alert.AlertType.WARNING, "Warning", message).showAndWait(); }

    public static void error(String message) { if (recorded("ERROR", message)) return; make(Alert.AlertType.ERROR, "Error", message).showAndWait(); }

    /** Standard handler for exceptions coming out of the service layer. */
    public static void showFailure(RuntimeException e) {
        showFailure(e, null);
    }

    /** Same, with a leading sentence such as "Purchase was NOT saved." */
    public static void showFailure(RuntimeException e, String prefix) {
        String lead = prefix == null ? "" : prefix + "\n\n";
        if (e instanceof com.stockflow.exception.SessionExpiredException) {
            warn(e.getMessage());
            SceneNavigator.showLogin();
        } else if (e instanceof com.stockflow.exception.AppException) {
            warn(lead + e.getMessage());
        } else {
            e.printStackTrace();
            error(lead + "Something went wrong while accessing the database.\n\nDetails: " + e.getMessage());
        }
    }

    public static boolean confirm(String title, String message) {
        if (recorded("CONFIRM", title + " | " + message)) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(SceneNavigator.css());
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
