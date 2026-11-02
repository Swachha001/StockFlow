package com.stockflow.exception;

/** The logged-in account was deactivated, removed or had its role changed: the user must log in again. */
public class SessionExpiredException extends AppException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
