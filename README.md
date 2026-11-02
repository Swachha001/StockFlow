# StockFlow — Smart Retail Inventory & Store Management System

Java 25 · JavaFX (FXML) · Maven · MySQL 8 · JDBC · jBCrypt · IntelliJ IDEA

## 1. Requirements
JDK 25, MySQL 8.x (+ MySQL Workbench), IntelliJ IDEA (Maven is bundled with it).

## 2. Database setup — safe, nothing here deletes data
Run these in MySQL Workbench **as a MySQL administrator (root)**, in this order:

| # | Script | What it does |
|---|--------|--------------|
| 1 | `database/01_schema.sql` | Creates `stockflow_db` and its tables **only if they do not exist**. Safe to run again. |
| 2 | `database/02_seed_data.sql` | Users, categories, suppliers, customers, products. **First-time only** — aborts with an error if data already exists. |
| 3 | `database/03_test_data.sql` | *Optional* demo purchases/sales for the dashboard. Aborts if purchases/sales already exist. |
| 4 | `database/04_create_app_user.sql` | Creates the least-privileged account `stockflow_app` (SELECT/INSERT/UPDATE on `stockflow_db` only). **Edit the password in the script first.** |

`database/99_reset_database_DESTRUCTIVE.sql` permanently deletes `stockflow_db`. It does nothing unless you edit `'NO'` to `'YES'` inside it. It is never part of normal setup.

Already have the old `stockflow_db`? Do **not** run 02/03 again. Just run `04_create_app_user.sql` (and use `01_schema.sql` freely — it will not touch your tables). No schema changes were made, so no migration is needed.

## 3. Configure the connection (the app does not run as root)
1. Copy `db.properties.example` to `db.properties` **in the same folder as `pom.xml`**.
2. Set `db.user=stockflow_app` and `db.password=` the password you chose in step 4.
`db.properties` is git-ignored. If it is missing, the login screen tells you exactly what to do.
Lookup order: `-Dstockflow.db.*` / `STOCKFLOW_DB_*` environment variables → `-Dstockflow.config=FILE` → `./db.properties` → `~/.stockflow/db.properties`.
The example URL pins the MySQL session to Bangladesh time (+06:00), so "today" and all timestamps are right even if the MySQL server runs in another time zone.

## 4. Run (IntelliJ IDEA)
1. File → Open → select `pom.xml` → *Open as Project*. Set Project SDK to **JDK 25**; wait for the Maven import.
2. Run `com.stockflow.Launcher` (or Maven panel → Plugins → javafx → `javafx:run`). The working directory must be the project folder (IntelliJ's default).

## 5. Demo accounts (development only — change/delete before real use)
| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| manager | manager123 | MANAGER |
| staff | staff123 | STAFF |

## 6. Roles and sessions
- **ADMIN** everything · **MANAGER** everything except Users and Activity Logs · **STAFF** view products, sell, view/add customers.
- You cannot change your own role or deactivate yourself (ask another admin). The last active admin is protected.
- Session policy: the account is re-checked against the database every 30 seconds and before actions. If it is deactivated, or its role is changed by someone else, the user is told and returned to the login screen.

## 7. Automated tests (dedicated database — never touches `stockflow_db`)
One-time setup, as MySQL admin: run `database/05_create_test_database.sql` (creates `stockflow_test` + user `stockflow_test`/`stockflow_test_pw`).

```
mvn clean test                      # runs BackendIntegrationTest (32 tests); skipped if stockflow_test is unreachable
```
Other test account/URL: `-Dstockflow.test.db.url=… -Dstockflow.test.db.user=… -Dstockflow.test.db.password=…` or the `STOCKFLOW_TEST_DB_URL|USER|PASSWORD` environment variables. The database name **must end in `_test`** or the tests refuse to run. The tests rebuild the tables from `01_schema.sql` before running, create their own uniquely named data, and empty the database afterwards.

UI check (layout at 1000×640 / 1280×780 / 1000×560 + drives the Purchases and Sales screens, then verifies the database): `src/test/java/com/stockflow/ui/UiWorkflowCheck.java` — run its `main` from IntelliJ (needs a display; on a Linux server use `xvfb-run`).

## 8. Architecture
`JavaFX/FXML → controller → service → dao → JDBC → MySQL`. Purchases and sales run in one SQL transaction (all-or-nothing); products are row-locked in id order; money is `BigDecimal`/`DECIMAL`; every query is a `PreparedStatement`; passwords are BCrypt.

## 9. Main features
Dashboard with 7-day chart and low-stock list · Products, Categories, Suppliers, Customers (search, filters, deactivate instead of delete) · Purchases (multi-item, stock increases) · Sales / POS (cart, stock checks, invoices) · Reports (sales, purchases, inventory, estimated gross profit; today/week/month/custom) · Users and Activity Logs (admin) · Settings (change password).
