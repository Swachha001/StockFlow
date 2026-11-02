package com.stockflow.exception;

public class InsufficientStockException extends AppException {
    private final int available;
    private final int requested;

    public InsufficientStockException(String product, int available, int requested) {
        super("Insufficient stock" + (product == null ? "" : " for " + product) + ".\n\nAvailable: " + available
                + "\nRequested: " + requested);
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
}
