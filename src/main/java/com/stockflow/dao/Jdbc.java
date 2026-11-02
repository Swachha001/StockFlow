package com.stockflow.dao;

import com.stockflow.config.DatabaseConfig;
import com.stockflow.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Thin PreparedStatement helper: every query in the app goes through here with bound parameters. */
public final class Jdbc {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    private Jdbc() { }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    private static DatabaseException fail(SQLException e) {
        return new DatabaseException("Database error: " + e.getMessage(), e);
    }

    public static LocalDateTime ts(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = DatabaseConfig.getConnection()) {
            return query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public static <T> T one(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(c, sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static <T> T one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static int update(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public static int update(String sql, Object... params) {
        try (Connection c = DatabaseConfig.getConnection()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public static long insert(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public static long insert(String sql, Object... params) {
        try (Connection c = DatabaseConfig.getConnection()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    /** Runs work in one transaction: COMMIT on success, ROLLBACK on any exception. */
    public static <T> T tx(TxWork<T> work) {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }
}
