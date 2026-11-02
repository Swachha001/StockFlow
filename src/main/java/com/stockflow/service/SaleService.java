package com.stockflow.service;

import com.stockflow.dao.CustomerDAO;
import com.stockflow.dao.Jdbc;
import com.stockflow.dao.ProductDAO;
import com.stockflow.dao.SaleDAO;
import com.stockflow.exception.InsufficientStockException;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.*;
import com.stockflow.util.Fmt;
import com.stockflow.util.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaleService {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Sale> search(String q, LocalDate from, LocalDate to) {
        Session.require(Permission.SALES);
        return saleDAO.search(q == null ? "" : q.trim(), from, to);
    }

    public List<SaleItem> items(Sale s) {
        Session.require(Permission.SALES);
        return saleDAO.findItems(s.getId());
    }

    public static BigDecimal total(List<SaleItem> items) {
        return items.stream().map(SaleItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks stock, saves sale + items and decreases stock in ONE transaction.
     * Prices are re-read from the database, so the client cannot tamper with them.
     */
    public Sale create(Customer customer, List<SaleItem> requested) {
        Session.require(Permission.SALES);
        if (requested == null || requested.isEmpty()) throw new ValidationException("The cart is empty.");

        Map<Integer, Integer> qtyByProduct = new LinkedHashMap<>();
        for (SaleItem i : requested) {
            if (i.getQuantity() <= 0) throw new ValidationException("Quantity must be greater than 0.");
            qtyByProduct.merge(i.getProductId(), i.getQuantity(), Integer::sum);
        }
        if (customer != null && customer.getId() > 0) {
            Customer c = customerDAO.findById(customer.getId());
            if (c == null || !c.isActive()) throw new ValidationException("Selected customer is not available.");
        }

        List<Integer> productIds = new ArrayList<>(qtyByProduct.keySet());
        productIds.sort(Comparator.naturalOrder()); // consistent lock order avoids deadlocks

        Sale sale = Jdbc.tx(c -> {
            Sale s = new Sale();
            s.setCustomerId(customer == null || customer.getId() <= 0 ? null : customer.getId());
            s.setUserId(Session.user().getId());
            List<SaleItem> items = new ArrayList<>();
            for (int productId : productIds) {
                int qty = qtyByProduct.get(productId);
                Product p = productDAO.lockForUpdate(c, productId);
                if (p == null || !p.isActive()) throw new ValidationException("A product in the cart is no longer available.");
                if (p.getQuantity() < qty || !productDAO.decreaseStock(c, productId, qty)) {
                    throw new InsufficientStockException(p.getName(), p.getQuantity(), qty);
                }
                SaleItem item = new SaleItem();
                item.setProductId(productId);
                item.setProductName(p.getName());
                item.setSku(p.getSku());
                item.setQuantity(qty);
                item.setUnitPrice(p.getSellingPrice());
                item.setUnitCost(p.getCostPrice());
                items.add(item);
            }
            s.setItems(items);
            s.setTotalAmount(total(items));
            int id = saleDAO.insert(c, s, "TMP-" + System.nanoTime());
            s.setId(id);
            s.setInvoiceNo(Fmt.invoiceNo(id));
            saleDAO.updateInvoice(c, id, s.getInvoiceNo());
            for (SaleItem item : items) {
                item.setSaleId(id);
                saleDAO.insertItem(c, item);
            }
            return s;
        });
        sale.setCustomerName(customer == null || customer.getId() <= 0 ? null : customer.getName());
        ActivityService.log("Sale created", sale.getInvoiceNo() + ", total " + Fmt.money(sale.getTotalAmount()));
        return sale;
    }
}
