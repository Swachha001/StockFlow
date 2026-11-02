package com.stockflow.dao;

import com.stockflow.model.Supplier;

public class SupplierDAO extends ContactDAO<Supplier> {
    public SupplierDAO() { super("suppliers"); }

    @Override protected Supplier create() { return new Supplier(); }
}
