package com.stockflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int id, userId;
    private Integer customerId;
    private String invoiceNo, customerName, userName;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDateTime saleDate;
    private List<SaleItem> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName == null ? "Walk-in Customer" : customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }

    @Override public String toString() { return invoiceNo + " (" + totalAmount + ")"; }
}
