package com.stockflow.service;

import com.stockflow.dao.Jdbc;
import com.stockflow.dao.ProductDAO;
import com.stockflow.dao.PurchaseDAO;
import com.stockflow.dao.SupplierDAO;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.*;
import com.stockflow.util.Check;
import com.stockflow.util.Fmt;
import com.stockflow.util.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PurchaseService {
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    public List<Purchase> search(String q, LocalDate from, LocalDate to) {
        Session.require(Permission.PURCHASES);
        return purchaseDAO.search(q == null ? "" : q.trim(), from, to);
    }

    public List<PurchaseItem> items(Purchase p) {
        Session.require(Permission.PURCHASES);
        return purchaseDAO.findItems(p.getId());
    }

    public static BigDecimal total(List<PurchaseItem> items) {
        return items.stream().map(PurchaseItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Saves the purchase + its items and increases stock in ONE transaction (all or nothing).
     * Stock is increased exactly once per product per purchase; if anything fails nothing is changed.
     * Returns the new purchase id only after the transaction has committed.
     */
    public int create(Supplier supplier, List<PurchaseItem> items, String note) {
        Session.require(Permission.PURCHASES);
        if (supplier == null) throw new ValidationException("Select a supplier.");
        if (items == null || items.isEmpty()) throw new ValidationException("Add at least one item.");

        // TreeMap => products are locked in ascending id order, so two purchases can never deadlock each other.
        Map<Integer, Integer> qtyByProduct = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItem i : items) {
            if (i.getQuantity() <= 0) throw new ValidationException("Quantity must be greater than 0.");
            if (i.getQuantity() > Check.MAX_QUANTITY) {
                throw new ValidationException("Quantity is too large (maximum " + Fmt.number(Check.MAX_QUANTITY) + " per line).");
            }
            Check.nonNegative(i.getUnitCost(), "Unit cost");
            BigDecimal subtotal = i.getSubtotal();
            if (subtotal.compareTo(Check.MAX_TOTAL_AMOUNT) > 0) throw new ValidationException("A line total is too large.");
            total = total.add(subtotal);
            qtyByProduct.merge(i.getProductId(), i.getQuantity(), Integer::sum);
        }
        if (total.compareTo(Check.MAX_TOTAL_AMOUNT) > 0) throw new ValidationException("The purchase total is too large.");

        Supplier current = supplierDAO.findById(supplier.getId());
        if (current == null || !current.isActive()) {
            throw new ValidationException("The selected supplier is no longer available (deleted or deactivated). Choose another supplier.");
        }

        Purchase purchase = new Purchase();
        purchase.setSupplierId(supplier.getId());
        purchase.setUserId(Session.user().getId());
        purchase.setTotalAmount(total);
        purchase.setNote(Check.maxLen(Check.blankToNull(note), 255, "Note"));

        int id = Jdbc.tx(c -> {
            for (Map.Entry<Integer, Integer> e : qtyByProduct.entrySet()) {
                Product p = productDAO.lockForUpdate(c, e.getKey());
                if (p == null) throw new ValidationException("A product in this purchase no longer exists.");
                if (!p.isActive()) throw new ValidationException("Product is no longer active: " + p.getName() + ". Remove it from the list.");
                if ((long) p.getQuantity() + e.getValue() > Integer.MAX_VALUE) {
                    throw new ValidationException("Stock of " + p.getName() + " would exceed the maximum allowed quantity.");
                }
            }
            int purchaseId = purchaseDAO.insert(c, purchase);
            for (PurchaseItem item : items) {
                item.setPurchaseId(purchaseId);
                purchaseDAO.insertItem(c, item);
            }
            for (Map.Entry<Integer, Integer> e : qtyByProduct.entrySet()) {
                productDAO.increaseStock(c, e.getKey(), e.getValue());
            }
            return purchaseId;
        });
        ActivityService.log("Purchase created", Fmt.purchaseRef(id) + " from " + supplier.getName() + ", total " + Fmt.money(total));
        return id;
    }
}
