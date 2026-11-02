package com.stockflow.config;

import com.stockflow.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads database settings. Lookup order (first found wins, later sources never override an earlier one):
 * <ol>
 *   <li>JVM properties / environment: -Dstockflow.db.url|user|password  or  STOCKFLOW_DB_URL|USER|PASSWORD</li>
 *   <li>file given by -Dstockflow.config=PATH or STOCKFLOW_CONFIG=PATH</li>
 *   <li>./db.properties (the folder the app is started from — the project folder in IntelliJ)</li>
 *   <li>~/.stockflow/db.properties</li>
 *   <li>db.properties on the classpath (optional, git-ignored)</li>
 * </ol>
 */
public final class DatabaseConfig {

    private static Properties props;

    private DatabaseConfig() {
    }

    private static String setting(String key) {
        String v = System.getProperty("stockflow." + key);
        if (v == null || v.isBlank()) v = System.getenv("STOCKFLOW_" + key.replace('.', '_').toUpperCase());
        return v == null || v.isBlank() ? null : v;
    }

    private static Properties readFile(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new DatabaseException("Cannot read database settings file " + file + ": " + e.getMessage(), e);
        }
    }

    private static Properties locate() {
        String explicit = setting("config");
        if (explicit != null) {
            Path f = Path.of(explicit);
            if (!Files.isRegularFile(f)) throw new DatabaseException("Database settings file not found: " + f.toAbsolutePath());
            return readFile(f);
        }
        Path local = Path.of("db.properties");
        if (Files.isRegularFile(local)) return readFile(local);
        Path home = Path.of(System.getProperty("user.home", "."), ".stockflow", "db.properties");
        if (Files.isRegularFile(home)) return readFile(home);
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p;
            }
        } catch (IOException e) {
            throw new DatabaseException("Cannot read db.properties from the classpath", e);
        }
        return null;
    }

    private static synchronized Properties props() {
        if (props == null) {
            Properties p = new Properties();
            if (setting("db.url") == null) {
                Properties file = locate();
                if (file == null) {
                    throw new DatabaseException("Database settings not found. Copy db.properties.example to db.properties "
                            + "(next to pom.xml), enter your MySQL application user and password, and start the app again. "
                            + "Looked in: " + Path.of("db.properties").toAbsolutePath() + " and ~/.stockflow/db.properties");
                }
                p.putAll(file);
            }
            for (String key : new String[]{"db.url", "db.user", "db.password"}) {
                String override = setting(key);
                if (override != null) p.setProperty(key, override);
            }
            if (p.getProperty("db.url") == null || p.getProperty("db.user") == null) {
                throw new DatabaseException("Database settings incomplete: db.url and db.user are required.");
            }
            props = p;
        }
        return props;
    }

    /** Forget cached settings (used by tests that switch databases). */
    public static synchronized void reset() {
        props = null;
    }

    public static String getUrl() {
        return props().getProperty("db.url");
    }

    public static Connection getConnection() throws SQLException {
        Properties p = props();
        return DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password", ""));
    }

    public static void testConnection() {
        try (Connection c = getConnection()) {
            if (!c.isValid(3)) throw new DatabaseException("Connection is not valid.");
        } catch (SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }
    }
}
