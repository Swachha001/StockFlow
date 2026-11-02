package com.stockflow.controller;

import com.stockflow.model.Permission;
import com.stockflow.model.Purchase;
import com.stockflow.model.ReportModels.HistoryRow;
import com.stockflow.model.Supplier;
import com.stockflow.service.SupplierService;
import com.stockflow.util.Fmt;
import com.stockflow.util.Session;

import java.util.List;

public class SuppliersController extends ContactController<Supplier> {
    private final SupplierService service = new SupplierService();

    @Override protected String singular() { return "Supplier"; }
    @Override protected String plural() { return "Suppliers"; }
    @Override protected String subtitle() { return "Companies you buy stock from"; }
    @Override protected Supplier newContact() { return new Supplier(); }
    @Override protected List<Supplier> load(String q, boolean inactive) { return service.search(q, inactive); }
    @Override protected void save(Supplier s) { service.save(s); }
    @Override protected void setActive(Supplier s, boolean active) { service.setActive(s, active); }
    @Override protected String historyTitle() { return "Purchase History"; }
    @Override protected boolean canAdd() { return Session.can(Permission.SUPPLIERS); }
    @Override protected boolean canManage() { return Session.can(Permission.SUPPLIERS); }

    @Override
    protected List<HistoryRow> history(Supplier s) {
        return service.history(s).stream()
                .map((Purchase p) -> new HistoryRow(Fmt.purchaseRef(p.getId()), p.getPurchaseDate(), p.getTotalAmount(), p.getUserName()))
                .toList();
    }
}
