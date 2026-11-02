package com.stockflow.dao;

import com.stockflow.model.Role;
import com.stockflow.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserDAO {

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(Jdbc.ts(rs, "created_at"));
        return u;
    }

    public User findByUsername(String username) {
        return Jdbc.one("SELECT * FROM users WHERE username = ?", UserDAO::map, username);
    }

    public User findById(int id) {
        return Jdbc.one("SELECT * FROM users WHERE id = ?", UserDAO::map, id);
    }

    public List<User> search(String q) {
        String like = "%" + q + "%";
        return Jdbc.query("SELECT * FROM users WHERE username LIKE ? OR full_name LIKE ? ORDER BY id", UserDAO::map, like, like);
    }

    public boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    public int insert(User u) {
        return (int) Jdbc.insert("INSERT INTO users (username, password_hash, full_name, role, active) VALUES (?,?,?,?,?)",
                u.getUsername(), u.getPasswordHash(), u.getFullName(), u.getRole().name(), u.isActive());
    }

    /** Updates only name and role (never 'active'), so a stale table row cannot undo another admin's change. */
    public void updateProfile(int id, String fullName, Role role) {
        Jdbc.update("UPDATE users SET full_name = ?, role = ? WHERE id = ?", fullName, role.name(), id);
    }

    public void setActive(int id, boolean active) {
        Jdbc.update("UPDATE users SET active = ? WHERE id = ?", active, id);
    }

    public void updatePassword(int id, String hash) {
        Jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", hash, id);
    }

    public int countActiveAdmins() {
        Long n = Jdbc.one("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = TRUE", rs -> rs.getLong(1));
        return n == null ? 0 : n.intValue();
    }
}
