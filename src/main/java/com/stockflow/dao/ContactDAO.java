package com.stockflow.dao;

import com.stockflow.model.Contact;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Shared CRUD for suppliers and customers. The table name is a constant supplied by the subclass, never user input. */
public abstract class ContactDAO<T extends Contact> {
    private final String table;

    protected ContactDAO(String table) {
        this.table = table;
    }

    protected abstract T create();

    protected T map(ResultSet rs) throws SQLException {
        T c = create();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setActive(rs.getBoolean("active"));
        c.setCreatedAt(Jdbc.ts(rs, "created_at"));
        return c;
    }

    public List<T> search(String q, boolean includeInactive) {
        String like = "%" + q + "%";
        return Jdbc.query("SELECT * FROM " + table + " WHERE (name LIKE ? OR phone LIKE ? OR email LIKE ?)"
                + (includeInactive ? "" : " AND active = TRUE") + " ORDER BY name", this::map, like, like, like);
    }

    public T findById(int id) {
        return Jdbc.one("SELECT * FROM " + table + " WHERE id = ?", this::map, id);
    }

    public int insert(T c) {
        return (int) Jdbc.insert("INSERT INTO " + table + " (name, phone, email, address) VALUES (?,?,?,?)",
                c.getName(), c.getPhone(), c.getEmail(), c.getAddress());
    }

    public void update(T c) {
        Jdbc.update("UPDATE " + table + " SET name = ?, phone = ?, email = ?, address = ? WHERE id = ?",
                c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getId());
    }

    public void setActive(int id, boolean active) {
        Jdbc.update("UPDATE " + table + " SET active = ? WHERE id = ?", active, id);
    }
}
