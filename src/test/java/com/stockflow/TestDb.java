package com.stockflow;

import com.stockflow.config.DatabaseConfig;
import com.stockflow.service.AuthService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a throw-away database for automated tests from the real database/01_schema.sql.
 * SAFETY: refuses to touch any database whose name does not end with "_test", so it can never wipe stockflow_db.
 * Settings (system property, then environment variable, then default):
 *   stockflow.test.db.url      / STOCKFLOW_TEST_DB_URL       (default jdbc:mysql://localhost:3306/stockflow_test?...)
 *   stockflow.test.db.user     / STOCKFLOW_TEST_DB_USER      (default stockflow_test)
 *   stockflow.test.db.password / STOCKFLOW_TEST_DB_PASSWORD  (default stockflow_test_pw)
 */
public final class TestDb {
    public static final String ADMIN_PW = "admin-test-pw", MANAGER_PW = "manager-test-pw", STAFF_PW = "staff-test-pw";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/stockflow_test?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Dhaka&characterEncoding=UTF-8&sessionVariables=time_zone='%2B06:00'";

    private TestDb() { }

    private static String setting(String key, String def) {
        String v = System.getProperty("stockflow.test." + key);
        if (v == null) v = System.getenv("STOCKFLOW_TEST_" + key.replace('.', '_').toUpperCase());
        return v == null || v.isBlank() ? def : v;
    }

    public static String url() { return setting("db.url", DEFAULT_URL); }
    public static String user() { return setting("db.user", "stockflow_test"); }
    public static String password() { return setting("db.password", "stockflow_test_pw"); }

    public static String databaseName() {
        Matcher m = Pattern.compile("^jdbc:mysql://[^/]+/([^?]+)").matcher(url());
        if (!m.find()) throw new IllegalStateException("Cannot read database name from test URL: " + url());
        return m.group(1);
    }

    /** True if the test database can be reached (tests are skipped otherwise). */
    public static boolean available() {
        try (Connection c = DriverManager.getConnection(url(), user(), password())) {
            return c.isValid(3);
        } catch (SQLException e) {
            System.err.println("[TestDb] test database not reachable: " + e.getMessage().lines().findFirst().orElse(""));
            return false;
        }
    }

    private static void guard() {
        if (!databaseName().endsWith("_test")) {
            throw new IllegalStateException("REFUSING to run: test database name must end with _test, but is '" + databaseName() + "'.");
        }
    }

    /** Point the application at the test database. */
    public static void useForApp() {
        guard();
        System.setProperty("stockflow.db.url", url());
        System.setProperty("stockflow.db.user", user());
        System.setProperty("stockflow.db.password", password());
        DatabaseConfig.reset();
    }

    /** Drops every table in the test DB, recreates the schema from database/01_schema.sql, adds 3 test users. */
    public static void rebuild() throws Exception {
        guard();
        useForApp();
        try (Connection c = DatabaseConfig.getConnection(); Statement st = c.createStatement()) {
            dropAll(st);
            for (String sql : schemaStatements()) st.execute(sql);
            insertUser(c, "admin", ADMIN_PW, "Test Admin", "ADMIN");
            insertUser(c, "manager", MANAGER_PW, "Test Manager", "MANAGER");
            insertUser(c, "staff", STAFF_PW, "Test Staff", "STAFF");
        }
    }

    public static void dropAllTables() throws Exception {
        guard();
        useForApp();
        try (Connection c = DatabaseConfig.getConnection(); Statement st = c.createStatement()) {
            dropAll(st);
        }
    }

    private static void dropAll(Statement st) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (var rs = st.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) tables.add(rs.getString(1));
        }
        st.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : tables) st.execute("DROP TABLE IF EXISTS `" + t + "`");
        st.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private static void insertUser(Connection c, String username, String pw, String name, String role) throws SQLException {
        try (var ps = c.prepareStatement("INSERT INTO users (username, password_hash, full_name, role) VALUES (?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, AuthService.hash(pw));
            ps.setString(3, name);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }

    /** Statements of the real schema file, minus CREATE DATABASE / USE (the connection already points at the test DB). */
    private static List<String> schemaStatements() throws Exception {
        Path file = Path.of("database", "01_schema.sql");
        if (!Files.isRegularFile(file)) file = Path.of("..", "database", "01_schema.sql");
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (!line.strip().startsWith("--")) sb.append(line).append('\n');
        }
        List<String> out = new ArrayList<>();
        for (String stmt : sb.toString().split(";")) {
            String t = stmt.strip();
            if (t.isEmpty() || t.toUpperCase().startsWith("CREATE DATABASE") || t.toUpperCase().startsWith("USE ")) continue;
            out.add(t);
        }
        return out;
    }
}
