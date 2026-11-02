package com.stockflow.dao;

import com.stockflow.model.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CategoryDAO {

    private static Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setActive(rs.getBoolean("active"));
        c.setProductCount(rs.getInt("product_count"));
        return c;
    }

    private static final String BASE = "SELECT c.*, (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.active = TRUE) AS product_count FROM categories c";

    public List<Category> search(String q, boolean includeInactive) {
        String like = "%" + q + "%";
        return Jdbc.query(BASE + " WHERE (c.name LIKE ? OR c.description LIKE ?)"
                + (includeInactive ? "" : " AND c.active = TRUE") + " ORDER BY c.name", CategoryDAO::map, like, like);
    }

    public boolean nameExists(String name, int excludeId) {
        return Jdbc.one("SELECT id FROM categories WHERE name = ? AND id <> ?", rs -> rs.getInt(1), name, excludeId) != null;
    }

    public int insert(Category c) {
        return (int) Jdbc.insert("INSERT INTO categories (name, description) VALUES (?,?)", c.getName(), c.getDescription());
    }

    public void update(Category c) {
        Jdbc.update("UPDATE categories SET name = ?, description = ? WHERE id = ?", c.getName(), c.getDescription(), c.getId());
    }

    public void setActive(int id, boolean active) {
        Jdbc.update("UPDATE categories SET active = ? WHERE id = ?", active, id);
    }
}
