-- Creates the least-privileged MySQL account the app should use (instead of root).
-- Run once as a MySQL administrator (e.g. root) AFTER 01_schema.sql.
-- 1) Change the password below (and use the same one in db.properties).
-- The account can only read/insert/update rows in stockflow_db — it cannot drop tables or touch other databases.
-- (The app never deletes rows: records are deactivated instead, so DELETE is intentionally not granted.)
CREATE USER IF NOT EXISTS 'stockflow_app'@'localhost' IDENTIFIED BY 'LeoSwapnil10@10';
GRANT SELECT, INSERT, UPDATE ON stockflow_db.* TO 'stockflow_app'@'localhost';
FLUSH PRIVILEGES;
