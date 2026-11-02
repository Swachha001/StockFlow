package com.stockflow.service;

import com.stockflow.dao.CustomerDAO;
import com.stockflow.dao.SaleDAO;
import com.stockflow.model.Customer;
import com.stockflow.model.Permission;
import com.stockflow.model.Sale;
import com.stockflow.util.Session;

import java.util.List;

public class CustomerService {
    private final CustomerDAO dao = new CustomerDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    public List<Customer> search(String q, boolean includeInactive) {
        Session.require(Permission.CUSTOMERS_VIEW);
        return dao.search(q == null ? "" : q.trim(), includeInactive);
    }

    public void save(Customer c) {
        Session.require(c.getId() == 0 ? Permission.CUSTOMERS_ADD : Permission.CUSTOMERS_MANAGE);
        ContactValidator.normalize(c, "Customer");
        if (c.getId() == 0) {
            c.setId(dao.insert(c));
            ActivityService.log("Customer added", c.getName());
        } else {
            dao.update(c);
            ActivityService.log("Customer updated", c.getName());
        }
    }

    public void setActive(Customer c, boolean active) {
        Session.require(Permission.CUSTOMERS_MANAGE);
        dao.setActive(c.getId(), active);
        ActivityService.log(active ? "Customer activated" : "Customer deactivated", c.getName());
    }

    public List<Sale> history(Customer c) {
        Session.require(Permission.CUSTOMERS_VIEW);
        return saleDAO.findByCustomer(c.getId());
    }
}
