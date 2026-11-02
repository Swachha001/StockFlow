package com.stockflow.controller;

import com.stockflow.model.Customer;
import com.stockflow.model.Permission;
import com.stockflow.model.ReportModels.HistoryRow;
import com.stockflow.model.Sale;
import com.stockflow.service.CustomerService;
import com.stockflow.util.Session;

import java.util.List;

public class CustomersController extends ContactController<Customer> {
    private final CustomerService service = new CustomerService();

    @Override protected String singular() { return "Customer"; }
    @Override protected String plural() { return "Customers"; }
    @Override protected String subtitle() { return "People who buy from your store"; }
    @Override protected Customer newContact() { return new Customer(); }
    @Override protected List<Customer> load(String q, boolean inactive) { return service.search(q, inactive); }
    @Override protected void save(Customer c) { service.save(c); }
    @Override protected void setActive(Customer c, boolean active) { service.setActive(c, active); }
    @Override protected String historyTitle() { return "Sales History"; }
    @Override protected boolean canAdd() { return Session.can(Permission.CUSTOMERS_ADD); }
    @Override protected boolean canManage() { return Session.can(Permission.CUSTOMERS_MANAGE); }

    @Override
    protected List<HistoryRow> history(Customer c) {
        return service.history(c).stream()
                .map((Sale s) -> new HistoryRow(s.getInvoiceNo(), s.getSaleDate(), s.getTotalAmount(), s.getUserName()))
                .toList();
    }
}
