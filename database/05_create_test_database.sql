-- OPTIONAL: dedicated database + account for the automated tests (src/test/java).
-- The tests rebuild every table in stockflow_test on each run — they NEVER touch stockflow_db.
CREATE DATABASE IF NOT EXISTS stockflow_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'stockflow_test'@'localhost' IDENTIFIED BY 'stockflow_test_pw';
GRANT ALL PRIVILEGES ON stockflow_test.* TO 'stockflow_test'@'localhost';
FLUSH PRIVILEGES;
