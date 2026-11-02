package com.stockflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Purchase {
    private int id, supplierId, userId;
    private String supplierName, userName, note;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDateTime purchaseDate;
    private List<PurchaseItem> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }
    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }

    @Override public String toString() { return "Purchase #" + id + " (" + totalAmount + ")"; }
}
