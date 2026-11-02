package com.stockflow.dao;

import com.stockflow.model.Purchase;
import com.stockflow.model.PurchaseItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    private static final String BASE = "SELECT pu.*, s.name AS supplier_name, u.full_name AS user_name FROM purchases pu "
            + "JOIN suppliers s ON s.id = pu.supplier_id JOIN users u ON u.id = pu.user_id";

    private static Purchase map(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setUserId(rs.getInt("user_id"));
        p.setUserName(rs.getString("user_name"));
        p.setTotalAmount(rs.getBigDecimal("total_amount"));
        p.setPurchaseDate(Jdbc.ts(rs, "purchase_date"));
        p.setNote(rs.getString("note"));
        return p;
    }

    private static PurchaseItem mapItem(ResultSet rs) throws SQLException {
        PurchaseItem i = new PurchaseItem();
        i.setId(rs.getInt("id"));
        i.setPurchaseId(rs.getInt("purchase_id"));
        i.setProductId(rs.getInt("product_id"));
        i.setProductName(rs.getString("name"));
        i.setSku(rs.getString("sku"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitCost(rs.getBigDecimal("unit_cost"));
        return i;
    }

    public int insert(Connection c, Purchase p) {
        return (int) Jdbc.insert(c, "INSERT INTO purchases (supplier_id, user_id, total_amount, note) VALUES (?,?,?,?)",
                p.getSupplierId(), p.getUserId(), p.getTotalAmount(), p.getNote());
    }

    public void insertItem(Connection c, PurchaseItem i) {
        Jdbc.insert(c, "INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, subtotal) VALUES (?,?,?,?,?)",
                i.getPurchaseId(), i.getProductId(), i.getQuantity(), i.getUnitCost(), i.getSubtotal());
    }

    public List<Purchase> search(String q, LocalDate from, LocalDate to) {
        String like = "%" + q + "%";
        StringBuilder sql = new StringBuilder(BASE + " WHERE (CAST(pu.id AS CHAR) LIKE ? OR CONCAT('PUR-', LPAD(pu.id, 4, '0')) LIKE ? "
                + "OR s.name LIKE ? OR DATE_FORMAT(pu.purchase_date, '%d-%m-%Y') LIKE ?)");
        List<Object> args = new ArrayList<>(List.of(like, like, like, like));
        if (from != null && to != null) {
            sql.append(" AND DATE(pu.purchase_date) BETWEEN ? AND ?");
            args.add(from);
            args.add(to);
        }
        sql.append(" ORDER BY pu.purchase_date DESC, pu.id DESC LIMIT 500");
        return Jdbc.query(sql.toString(), PurchaseDAO::map, args.toArray());
    }

    public List<Purchase> findBySupplier(int supplierId) {
        return Jdbc.query(BASE + " WHERE pu.supplier_id = ? ORDER BY pu.purchase_date DESC, pu.id DESC", PurchaseDAO::map, supplierId);
    }

    public List<PurchaseItem> findItems(int purchaseId) {
        return Jdbc.query("SELECT i.*, p.name, p.sku FROM purchase_items i JOIN products p ON p.id = i.product_id "
                + "WHERE i.purchase_id = ? ORDER BY i.id", PurchaseDAO::mapItem, purchaseId);
    }
}
