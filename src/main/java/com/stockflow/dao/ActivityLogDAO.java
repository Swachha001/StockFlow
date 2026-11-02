package com.stockflow.dao;

import com.stockflow.model.ActivityLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ActivityLogDAO {

    private static ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog l = new ActivityLog();
        l.setId(rs.getInt("id"));
        int uid = rs.getInt("user_id");
        l.setUserId(rs.wasNull() ? null : uid);
        l.setUsername(rs.getString("username"));
        l.setAction(rs.getString("action"));
        l.setDetails(rs.getString("details"));
        l.setCreatedAt(Jdbc.ts(rs, "created_at"));
        return l;
    }

    public void insert(Integer userId, String action, String details) {
        String d = details == null ? null : (details.length() > 500 ? details.substring(0, 500) : details);
        Jdbc.insert("INSERT INTO activity_logs (user_id, action, details) VALUES (?,?,?)", userId, action, d);
    }

    public List<ActivityLog> search(String q) {
        String like = "%" + q + "%";
        return Jdbc.query("SELECT l.*, u.username FROM activity_logs l LEFT JOIN users u ON u.id = l.user_id "
                + "WHERE (l.action LIKE ? OR l.details LIKE ? OR u.username LIKE ?) ORDER BY l.id DESC LIMIT 500",
                ActivityLogDAO::map, like, like, like);
    }
}
