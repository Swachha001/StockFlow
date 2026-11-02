package com.stockflow.dao;

import com.stockflow.model.Customer;

public class CustomerDAO extends ContactDAO<Customer> {
    public CustomerDAO() { super("customers"); }

    @Override protected Customer create() { return new Customer(); }
}
