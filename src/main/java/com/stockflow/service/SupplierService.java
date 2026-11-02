package com.stockflow.service;

import com.stockflow.dao.PurchaseDAO;
import com.stockflow.dao.SupplierDAO;
import com.stockflow.model.Permission;
import com.stockflow.model.Purchase;
import com.stockflow.model.Supplier;
import com.stockflow.util.Session;

import java.util.List;

public class SupplierService {
    private final SupplierDAO dao = new SupplierDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();

    public List<Supplier> search(String q, boolean includeInactive) {
        Session.require(Permission.SUPPLIERS);
        return dao.search(q == null ? "" : q.trim(), includeInactive);
    }

    public void save(Supplier s) {
        Session.require(Permission.SUPPLIERS);
        ContactValidator.normalize(s, "Supplier");
        if (s.getId() == 0) {
            s.setId(dao.insert(s));
            ActivityService.log("Supplier added", s.getName());
        } else {
            dao.update(s);
            ActivityService.log("Supplier updated", s.getName());
        }
    }

    public void setActive(Supplier s, boolean active) {
        Session.require(Permission.SUPPLIERS);
        dao.setActive(s.getId(), active);
        ActivityService.log(active ? "Supplier activated" : "Supplier deactivated", s.getName());
    }

    public List<Purchase> history(Supplier s) {
        Session.require(Permission.SUPPLIERS);
        return purchaseDAO.findBySupplier(s.getId());
    }
}
