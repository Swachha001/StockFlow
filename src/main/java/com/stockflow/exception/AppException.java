package com.stockflow.exception;

/** A problem the user can understand and fix; its message is shown in the UI. */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
