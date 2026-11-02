package com.stockflow.service;

import com.stockflow.dao.Jdbc;
import com.stockflow.dao.ProductDAO;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.Permission;
import com.stockflow.model.Product;
import com.stockflow.util.Check;
import com.stockflow.util.Session;

import java.math.RoundingMode;
import java.util.List;

public class ProductService {
    private final ProductDAO dao = new ProductDAO();

    public List<Product> search(String q, Integer categoryId, boolean lowOnly, boolean includeInactive) {
        Session.require(Permission.PRODUCTS_VIEW);
        return dao.search(q == null ? "" : q.trim(), categoryId, lowOnly, includeInactive);
    }

    public List<Product> searchActive(String q) {
        return search(q, null, false, false);
    }

    public List<Product> lowStock() {
        Session.require(Permission.PRODUCTS_VIEW);
        return dao.findLowStock();
    }

    public void save(Product p) {
        Session.require(Permission.PRODUCTS_MANAGE);
        p.setSku(Check.maxLen(Check.required(p.getSku(), "SKU"), 40, "SKU").toUpperCase());
        p.setName(Check.maxLen(Check.required(p.getName(), "Product name"), 150, "Product name"));
        p.setBrand(Check.maxLen(Check.blankToNull(p.getBrand()), 60, "Brand"));
        if (p.getCategoryId() <= 0) throw new ValidationException("Category is required.");
        Check.nonNegative(p.getCostPrice(), "Cost price");
        Check.nonNegative(p.getSellingPrice(), "Selling price");
        p.setCostPrice(p.getCostPrice().setScale(2, RoundingMode.HALF_UP));
        p.setSellingPrice(p.getSellingPrice().setScale(2, RoundingMode.HALF_UP));
        if (p.getQuantity() < 0) throw new ValidationException("Quantity cannot be negative.");
        if (p.getReorderLevel() < 0) throw new ValidationException("Reorder level cannot be negative.");
        if (dao.skuExists(p.getSku(), p.getId())) throw new ValidationException("SKU already exists.");
        if (p.getId() == 0) {
            p.setId(dao.insert(p));
            ActivityService.log("Product added", p.getSku() + " - " + p.getName());
        } else {
            // Stock is only overwritten if the user really typed a different number, and only if nobody
            // (a purchase, a sale, another user) changed it since the form was opened — otherwise the edit
            // would silently undo that purchase/sale.
            boolean quantityEdited = p.getQuantity() != p.getLoadedQuantity();
            Jdbc.tx(c -> {
                Product current = dao.lockForUpdate(c, p.getId());
                if (current == null) throw new ValidationException("This product no longer exists.");
                if (quantityEdited && current.getQuantity() != p.getLoadedQuantity()) {
                    throw new ValidationException("The stock level changed while you were editing (it is now "
                            + current.getQuantity() + "). Close this form, reopen the product and try again.");
                }
                dao.update(c, p, quantityEdited);
                return null;
            });
            ActivityService.log("Product updated", p.getSku() + " - " + p.getName());
        }
    }

    public void setActive(Product p, boolean active) {
        Session.require(Permission.PRODUCTS_MANAGE);
        dao.setActive(p.getId(), active);
        ActivityService.log(active ? "Product activated" : "Product deactivated", p.getSku() + " - " + p.getName());
    }
}
