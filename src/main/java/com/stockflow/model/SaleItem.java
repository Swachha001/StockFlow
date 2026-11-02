package com.stockflow.model;

import java.math.BigDecimal;

public class SaleItem {
    private int id, saleId, productId, quantity;
    private String productName, sku;
    private BigDecimal unitPrice = BigDecimal.ZERO, unitCost = BigDecimal.ZERO;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    @Override public String toString() { return sku + " x" + quantity; }
}
