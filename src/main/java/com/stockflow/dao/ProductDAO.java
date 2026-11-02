package com.stockflow.dao;

import com.stockflow.model.Product;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String BASE = "SELECT p.*, c.name AS category_name, s.name AS supplier_name FROM products p "
            + "JOIN categories c ON c.id = p.category_id LEFT JOIN suppliers s ON s.id = p.supplier_id";

    private static Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        int sid = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : sid);
        p.setSupplierName(rs.getString("supplier_name"));
        p.setBrand(rs.getString("brand"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        p.setActive(rs.getBoolean("active"));
        p.setCreatedAt(Jdbc.ts(rs, "created_at"));
        return p;
    }

    public List<Product> search(String q, Integer categoryId, boolean lowOnly, boolean includeInactive) {
        String like = "%" + q + "%";
        StringBuilder sql = new StringBuilder(BASE + " WHERE (p.sku LIKE ? OR p.name LIKE ? OR p.brand LIKE ?)");
        List<Object> args = new ArrayList<>(List.of(like, like, like));
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            args.add(categoryId);
        }
        if (lowOnly) sql.append(" AND p.quantity <= p.reorder_level");
        if (!includeInactive) sql.append(" AND p.active = TRUE");
        sql.append(" ORDER BY p.name");
        return Jdbc.query(sql.toString(), ProductDAO::map, args.toArray());
    }

    public Product findById(int id) {
        return Jdbc.one(BASE + " WHERE p.id = ?", ProductDAO::map, id);
    }

    public List<Product> findLowStock() {
        return Jdbc.query(BASE + " WHERE p.active = TRUE AND p.quantity <= p.reorder_level ORDER BY p.quantity, p.name", ProductDAO::map);
    }

    public boolean skuExists(String sku, int excludeId) {
        return Jdbc.one("SELECT id FROM products WHERE sku = ? AND id <> ?", rs -> rs.getInt(1), sku, excludeId) != null;
    }

    public int insert(Product p) {
        return (int) Jdbc.insert("INSERT INTO products (sku, name, category_id, supplier_id, brand, cost_price, selling_price, quantity, reorder_level, active) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                p.getSku(), p.getName(), p.getCategoryId(), p.getSupplierId(), p.getBrand(), p.getCostPrice(),
                p.getSellingPrice(), p.getQuantity(), p.getReorderLevel(), p.isActive());
    }

    /** Updates product details; the stock quantity is written only when {@code updateQuantity} is true. */
    public void update(Connection c, Product p, boolean updateQuantity) {
        if (updateQuantity) {
            Jdbc.update(c, "UPDATE products SET sku = ?, name = ?, category_id = ?, supplier_id = ?, brand = ?, cost_price = ?, "
                            + "selling_price = ?, quantity = ?, reorder_level = ? WHERE id = ?",
                    p.getSku(), p.getName(), p.getCategoryId(), p.getSupplierId(), p.getBrand(), p.getCostPrice(),
                    p.getSellingPrice(), p.getQuantity(), p.getReorderLevel(), p.getId());
        } else {
            Jdbc.update(c, "UPDATE products SET sku = ?, name = ?, category_id = ?, supplier_id = ?, brand = ?, cost_price = ?, "
                            + "selling_price = ?, reorder_level = ? WHERE id = ?",
                    p.getSku(), p.getName(), p.getCategoryId(), p.getSupplierId(), p.getBrand(), p.getCostPrice(),
                    p.getSellingPrice(), p.getReorderLevel(), p.getId());
        }
    }

    public void setActive(int id, boolean active) {
        Jdbc.update("UPDATE products SET active = ? WHERE id = ?", active, id);
    }

    // ---- transactional helpers (used by purchase/sale services) ----

    /** Reads and row-locks a product inside a transaction. */
    public Product lockForUpdate(Connection c, int id) {
        return Jdbc.one(c, "SELECT p.*, '' AS category_name, '' AS supplier_name FROM products p WHERE p.id = ? FOR UPDATE",
                ProductDAO::map, id);
    }

    public void increaseStock(Connection c, int id, int qty) {
        Jdbc.update(c, "UPDATE products SET quantity = quantity + ? WHERE id = ?", qty, id);
    }

    /** Returns false if there was not enough stock (nothing is changed then). */
    public boolean decreaseStock(Connection c, int id, int qty) {
        return Jdbc.update(c, "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?", qty, id, qty) == 1;
    }
}
