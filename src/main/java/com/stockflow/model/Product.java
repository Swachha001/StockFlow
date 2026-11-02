package com.stockflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private int id, categoryId, quantity, reorderLevel = 5;
    /** Stock level shown when an edit form was opened; lets the save detect that stock changed meanwhile. */
    private int loadedQuantity;
    private Integer supplierId;
    private String sku, name, brand, categoryName, supplierName;
    private BigDecimal costPrice = BigDecimal.ZERO, sellingPrice = BigDecimal.ZERO;
    private boolean active = true;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getLoadedQuantity() { return loadedQuantity; }
    public void setLoadedQuantity(int loadedQuantity) { this.loadedQuantity = loadedQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() { return quantity <= reorderLevel; }
    public BigDecimal getStockValue() { return costPrice.multiply(BigDecimal.valueOf(quantity)); }

    public String getStatusText() {
        if (!active) return "Inactive";
        if (quantity == 0) return "✖ Out of Stock";
        return isLowStock() ? "⚠ Low Stock" : "In Stock";
    }

    @Override public String toString() { return sku + " — " + name; }
}
