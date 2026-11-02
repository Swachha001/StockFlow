package com.stockflow.dao;

import com.stockflow.model.Sale;
import com.stockflow.model.SaleItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    private static final String BASE = "SELECT s.*, c.name AS customer_name, u.full_name AS user_name FROM sales s "
            + "LEFT JOIN customers c ON c.id = s.customer_id JOIN users u ON u.id = s.user_id";

    private static Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNo(rs.getString("invoice_no"));
        int cid = rs.getInt("customer_id");
        s.setCustomerId(rs.wasNull() ? null : cid);
        s.setCustomerName(rs.getString("customer_name"));
        s.setUserId(rs.getInt("user_id"));
        s.setUserName(rs.getString("user_name"));
        s.setTotalAmount(rs.getBigDecimal("total_amount"));
        s.setSaleDate(Jdbc.ts(rs, "sale_date"));
        return s;
    }

    private static SaleItem mapItem(ResultSet rs) throws SQLException {
        SaleItem i = new SaleItem();
        i.setId(rs.getInt("id"));
        i.setSaleId(rs.getInt("sale_id"));
        i.setProductId(rs.getInt("product_id"));
        i.setProductName(rs.getString("name"));
        i.setSku(rs.getString("sku"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        i.setUnitCost(rs.getBigDecimal("unit_cost"));
        return i;
    }

    public int insert(Connection c, Sale s, String tempInvoice) {
        return (int) Jdbc.insert(c, "INSERT INTO sales (invoice_no, customer_id, user_id, total_amount) VALUES (?,?,?,?)",
                tempInvoice, s.getCustomerId(), s.getUserId(), s.getTotalAmount());
    }

    public void updateInvoice(Connection c, int id, String invoiceNo) {
        Jdbc.update(c, "UPDATE sales SET invoice_no = ? WHERE id = ?", invoiceNo, id);
    }

    public void insertItem(Connection c, SaleItem i) {
        Jdbc.insert(c, "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, unit_cost, subtotal) VALUES (?,?,?,?,?,?)",
                i.getSaleId(), i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getUnitCost(), i.getSubtotal());
    }

    public List<Sale> search(String q, LocalDate from, LocalDate to) {
        String like = "%" + q + "%";
        StringBuilder sql = new StringBuilder(BASE + " WHERE (s.invoice_no LIKE ? OR CAST(s.id AS CHAR) LIKE ? OR c.name LIKE ? "
                + "OR DATE_FORMAT(s.sale_date, '%d-%m-%Y') LIKE ?)");
        List<Object> args = new ArrayList<>(List.of(like, like, like, like));
        if (from != null && to != null) {
            sql.append(" AND DATE(s.sale_date) BETWEEN ? AND ?");
            args.add(from);
            args.add(to);
        }
        sql.append(" ORDER BY s.sale_date DESC, s.id DESC LIMIT 500");
        return Jdbc.query(sql.toString(), SaleDAO::map, args.toArray());
    }

    public List<Sale> findByCustomer(int customerId) {
        return Jdbc.query(BASE + " WHERE s.customer_id = ? ORDER BY s.sale_date DESC, s.id DESC", SaleDAO::map, customerId);
    }

    public List<SaleItem> findItems(int saleId) {
        return Jdbc.query("SELECT i.*, p.name, p.sku FROM sale_items i JOIN products p ON p.id = i.product_id "
                + "WHERE i.sale_id = ? ORDER BY i.id", SaleDAO::mapItem, saleId);
    }
}
