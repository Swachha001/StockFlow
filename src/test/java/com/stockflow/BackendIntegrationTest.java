package com.stockflow;

import com.stockflow.config.DatabaseConfig;
import com.stockflow.dao.Jdbc;
import com.stockflow.dao.ProductDAO;
import com.stockflow.exception.*;
import com.stockflow.model.*;
import com.stockflow.model.ReportModels.*;
import com.stockflow.service.*;
import com.stockflow.util.Session;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests against a DEDICATED database (default stockflow_test, see database/05_create_test_database.sql).
 * The database is rebuilt from database/01_schema.sql before the run and emptied afterwards; it can never be stockflow_db.
 * Skipped automatically when the test database is not reachable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class BackendIntegrationTest {
    final AuthService auth = new AuthService();
    final ProductService products = new ProductService();
    final CategoryService categories = new CategoryService();
    final SupplierService suppliers = new SupplierService();
    final CustomerService customers = new CustomerService();
    final PurchaseService purchases = new PurchaseService();
    final SaleService sales = new SaleService();
    final UserService users = new UserService();
    final DashboardService dashboard = new DashboardService();
    final ReportService reports = new ReportService();
    final ProductDAO productDAO = new ProductDAO();

    final String run = Long.toString(System.nanoTime(), 36).toUpperCase();
    final AtomicInteger seq = new AtomicInteger();
    Category category;
    Supplier supplier;

    @BeforeAll
    void setUpDatabase() throws Exception {
        assumeTrue(TestDb.available(), "Test database not reachable - run database/05_create_test_database.sql (see README). Tests skipped.");
        TestDb.rebuild();
        auth.login("admin", TestDb.ADMIN_PW);
        category = new Category();
        category.setName("Cat " + run);
        categories.save(category);
        supplier = new Supplier();
        supplier.setName("Supplier " + run);
        supplier.setPhone("01712345678");
        suppliers.save(supplier);
        auth.logout();
    }

    @AfterAll
    void cleanUp() throws Exception {
        if (TestDb.available()) {
            auth.logout();
            TestDb.dropAllTables(); // leave the test database empty
        }
    }

    @BeforeEach
    void loginAdmin() {
        Session.end();
        Session.setRevalidateIntervalMillis(30_000);
        auth.login("admin", TestDb.ADMIN_PW);
    }

    @AfterEach
    void logoutAfter() {
        Session.setRevalidateIntervalMillis(30_000);
        Session.end();
    }

    // ---------------------------------------------------------------- helpers
    Product newProduct(int qty, String cost, String price) {
        Product p = new Product();
        p.setSku("T-" + run + "-" + seq.incrementAndGet());
        p.setName("Test product " + seq.get());
        p.setCategoryId(category.getId());
        p.setCostPrice(new BigDecimal(cost));
        p.setSellingPrice(new BigDecimal(price));
        p.setQuantity(qty);
        p.setReorderLevel(2);
        products.save(p);
        return p;
    }

    PurchaseItem pItem(Product p, int qty, String cost) {
        PurchaseItem i = new PurchaseItem();
        i.setProductId(p.getId());
        i.setProductName(p.getName());
        i.setSku(p.getSku());
        i.setQuantity(qty);
        i.setUnitCost(new BigDecimal(cost));
        return i;
    }

    SaleItem sItem(Product p, int qty) {
        SaleItem i = new SaleItem();
        i.setProductId(p.getId());
        i.setQuantity(qty);
        return i;
    }

    int stock(Product p) { return productDAO.findById(p.getId()).getQuantity(); }

    long count(String table) { return Jdbc.one("SELECT COUNT(*) FROM " + table, rs -> rs.getLong(1)); }

    void exec(String sql) throws Exception {
        try (Connection c = DatabaseConfig.getConnection(); Statement st = c.createStatement()) { st.execute(sql); }
    }

    // ---------------------------------------------------------------- login
    @Test @DisplayName("01 login: correct, wrong, unknown, inactive")
    void login() {
        auth.logout();
        assertEquals(Role.ADMIN, auth.login("admin", TestDb.ADMIN_PW).getRole());
        auth.logout();
        assertEquals("Invalid username or password", assertThrows(AppException.class, () -> auth.login("admin", "nope")).getMessage());
        assertEquals("Invalid username or password", assertThrows(AppException.class, () -> auth.login("ghost", "x")).getMessage());
        auth.login("admin", TestDb.ADMIN_PW);
        users.create("inact" + run.toLowerCase(), "Inactive User", Role.STAFF, "secret1", "secret1");
        User u = users.search("inact" + run.toLowerCase()).get(0);
        users.setActive(u, false);
        auth.logout();
        assertThrows(AppException.class, () -> auth.login(u.getUsername(), "secret1"));
    }

    @Test @DisplayName("02 password change and reset")
    void passwords() {
        String name = "pw" + run.toLowerCase();
        users.create(name, "Pw User", Role.STAFF, "first-pw", "first-pw");
        User u = users.search(name).get(0);
        users.resetPassword(u, "second-pw", "second-pw");
        auth.logout();
        assertThrows(AppException.class, () -> auth.login(name, "first-pw"));
        auth.login(name, "second-pw");
        assertThrows(ValidationException.class, () -> auth.changePassword("WRONG", "third-pw", "third-pw"));
        assertThrows(ValidationException.class, () -> auth.changePassword("second-pw", "abc", "abc"));
        assertThrows(ValidationException.class, () -> auth.changePassword("second-pw", "third-pw", "mismatch"));
        auth.changePassword("second-pw", "third-pw", "third-pw");
        auth.logout();
        auth.login(name, "third-pw");
    }

    // ---------------------------------------------------------------- products
    @Test @DisplayName("03 products: add/edit/search/deactivate/validation")
    void productsCrud() {
        Product p = newProduct(10, "100.00", "150.00");
        assertEquals(p.getSku(), p.getSku().toUpperCase());
        Product dup = new Product();
        dup.setSku(p.getSku()); dup.setName("Dup"); dup.setCategoryId(category.getId());
        assertEquals("SKU already exists.", assertThrows(ValidationException.class, () -> products.save(dup)).getMessage());
        Product bad = new Product();
        bad.setSku("BAD-" + run); bad.setName("Bad"); bad.setCategoryId(category.getId()); bad.setSellingPrice(new BigDecimal("-5"));
        assertThrows(ValidationException.class, () -> products.save(bad));
        Product blank = new Product();
        blank.setSku("BL-" + run); blank.setName(" "); blank.setCategoryId(category.getId());
        assertThrows(ValidationException.class, () -> products.save(blank));
        Product big = new Product();
        big.setSku("BIG-" + run); big.setName("Big"); big.setCategoryId(category.getId()); big.setSellingPrice(new BigDecimal("99999999999999.00"));
        assertTrue(assertThrows(ValidationException.class, () -> products.save(big)).getMessage().contains("too large"));

        p.setLoadedQuantity(p.getQuantity());
        p.setName("Renamed " + run);
        products.save(p);
        assertEquals("Renamed " + run, productDAO.findById(p.getId()).getName());
        assertFalse(products.search("Renamed " + run, null, false, false).isEmpty());
        products.setActive(productDAO.findById(p.getId()), false);
        assertTrue(products.searchActive("Renamed " + run).isEmpty());
        assertFalse(products.search("Renamed " + run, null, false, true).isEmpty());
    }

    @Test @DisplayName("04 low stock is quantity <= reorder level")
    void lowStock() {
        Product p = newProduct(2, "10.00", "20.00"); // reorder level 2
        assertTrue(productDAO.findById(p.getId()).isLowStock());
        assertTrue(products.lowStock().stream().anyMatch(x -> x.getId() == p.getId()));
        Product ok = newProduct(3, "10.00", "20.00");
        assertFalse(products.lowStock().stream().anyMatch(x -> x.getId() == ok.getId()));
    }

    // ---------------------------------------------------------------- purchases (priority 0)
    @Test @DisplayName("10 purchase: saves purchase + items and increases stock exactly once")
    void purchaseHappyPath() {
        Product p = newProduct(10, "100.00", "150.00");
        long before = count("purchases");
        int id = purchases.create(supplier, List.of(pItem(p, 10, "95.50")), "note");
        assertEquals(20, stock(p));
        assertEquals(before + 1, count("purchases"));
        Purchase saved = purchases.search(com.stockflow.util.Fmt.purchaseRef(id), null, null).get(0);
        assertEquals(0, new BigDecimal("955.00").compareTo(saved.getTotalAmount()));
        assertEquals(1, purchases.items(saved).size());
        assertEquals(supplier.getId(), saved.getSupplierId());
        assertEquals("Test Admin", saved.getUserName());
    }

    @Test @DisplayName("11 purchase: duplicate cart lines for one product add up once each")
    void purchaseDuplicateLines() {
        Product p = newProduct(5, "10.00", "20.00");
        int id = purchases.create(supplier, List.of(pItem(p, 5, "10.00"), pItem(p, 7, "12.00")), null);
        assertEquals(17, stock(p)); // 5 + 5 + 7, not more
        Purchase saved = purchases.search(com.stockflow.util.Fmt.purchaseRef(id), null, null).get(0);
        assertEquals(2, purchases.items(saved).size());
        assertEquals(0, new BigDecimal("134.00").compareTo(saved.getTotalAmount())); // 50 + 84
    }

    @Test @DisplayName("12 purchase: money is exact (3 x 0.10 = 0.30)")
    void purchaseMoney() {
        Product p = newProduct(0, "0.10", "0.20");
        int id = purchases.create(supplier, List.of(pItem(p, 3, "0.10")), null);
        assertEquals(0, new BigDecimal("0.30").compareTo(purchases.search(com.stockflow.util.Fmt.purchaseRef(id), null, null).get(0).getTotalAmount()));
    }

    @Test @DisplayName("13 purchase: invalid input is rejected and changes nothing")
    void purchaseInvalid() {
        Product p = newProduct(10, "10.00", "20.00");
        long before = count("purchases"), itemsBefore = count("purchase_items");
        assertThrows(ValidationException.class, () -> purchases.create(null, List.of(pItem(p, 1, "1")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 0, "1")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, -3, "1")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 1, "-1")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 2_000_000, "1")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 1, "99999999999999")), null));
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 1_000_000, "9999999999.99"), pItem(p, 1_000_000, "9999999999.99")), null));
        assertEquals(10, stock(p));
        assertEquals(before, count("purchases"));
        assertEquals(itemsBefore, count("purchase_items"));
    }

    @Test @DisplayName("14 purchase: inactive product / inactive supplier / unknown product rejected, no changes")
    void purchaseStaleRecords() {
        Product p = newProduct(10, "10.00", "20.00");
        Product q = newProduct(10, "10.00", "20.00");
        products.setActive(productDAO.findById(q.getId()), false);
        long before = count("purchases");
        ValidationException e = assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(pItem(p, 1, "1"), pItem(q, 1, "1")), null));
        assertTrue(e.getMessage().contains("no longer active"), e.getMessage());
        PurchaseItem ghost = pItem(p, 1, "1");
        ghost.setProductId(987654);
        assertThrows(ValidationException.class, () -> purchases.create(supplier, List.of(ghost), null));
        assertEquals(10, stock(p));
        assertEquals(10, stock(q));
        assertEquals(before, count("purchases"));

        Supplier s2 = new Supplier();
        s2.setName("Temp supplier " + run); s2.setPhone("01812345678");
        suppliers.save(s2);
        suppliers.setActive(s2, false);
        assertTrue(assertThrows(ValidationException.class, () -> purchases.create(s2, List.of(pItem(p, 1, "1")), null)).getMessage().contains("supplier"));
        assertEquals(10, stock(p));
    }

    @Test @DisplayName("15 purchase: database failure after stock increase rolls EVERYTHING back")
    void purchaseRollbackOnDbFailure() throws Exception {
        Product a = newProduct(3, "10.00", "20.00");   // lower id -> stock updated first
        Product b = newProduct(4, "10.00", "20.00");
        // test-only CHECK constraint: the stock UPDATE that would set exactly 781 units fails inside the transaction
        exec("ALTER TABLE products ADD CONSTRAINT chk_test_781 CHECK (quantity <> 781)");
        try {
            long purchasesBefore = count("purchases"), itemsBefore = count("purchase_items"), logsBefore = count("activity_logs");
            DatabaseException e = assertThrows(DatabaseException.class,
                    () -> purchases.create(supplier, List.of(pItem(a, 10, "1.00"), pItem(b, 777, "1.00")), null));
            assertTrue(e.getMessage().contains("chk_test_781"), e.getMessage());
            assertEquals(3, stock(a), "stock of the product updated first must be rolled back");
            assertEquals(4, stock(b));
            assertEquals(purchasesBefore, count("purchases"));
            assertEquals(itemsBefore, count("purchase_items"));
            assertEquals(logsBefore, count("activity_logs"), "no 'Purchase created' log for a failed purchase");
        } finally {
            exec("ALTER TABLE products DROP CHECK chk_test_781");
        }
        // and a normal purchase works again afterwards
        purchases.create(supplier, List.of(pItem(a, 10, "1.00")), null);
        assertEquals(13, stock(a));
    }

    @Test @DisplayName("16 purchase: failure while inserting items rolls back purchase header")
    void purchaseRollbackOnItemInsert() throws Exception {
        Product a = newProduct(3, "10.00", "20.00");
        exec("ALTER TABLE purchase_items ADD CONSTRAINT chk_test_013 CHECK (unit_cost <> 0.13)");
        try {
            long before = count("purchases"), items = count("purchase_items");
            assertThrows(DatabaseException.class, () -> purchases.create(supplier, List.of(pItem(a, 1, "1.00"), pItem(a, 1, "0.13")), null));
            assertEquals(before, count("purchases"));
            assertEquals(items, count("purchase_items"));
            assertEquals(3, stock(a));
        } finally {
            exec("ALTER TABLE purchase_items DROP CHECK chk_test_013");
        }
    }

    @Test @DisplayName("17 purchase: logged in activity log")
    void purchaseLogged() {
        Product p = newProduct(1, "1.00", "2.00");
        int id = purchases.create(supplier, List.of(pItem(p, 1, "1.00")), null);
        assertTrue(ActivityService.search("PUR-").stream().anyMatch(l -> l.getAction().equals("Purchase created")
                && l.getDetails().startsWith(com.stockflow.util.Fmt.purchaseRef(id))));
    }

    @Test @DisplayName("18 purchase: concurrent purchases never lose an increment")
    void purchaseConcurrent() throws Exception {
        Product p = newProduct(0, "1.00", "2.00");
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Integer>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) fs.add(pool.submit(() -> purchases.create(supplier, List.of(pItem(p, 5, "1.00")), null)));
        for (Future<Integer> f : fs) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(60, stock(p));
    }

    // ---------------------------------------------------------------- sales
    @Test @DisplayName("20 sale: stock decreases, price comes from the database")
    void saleHappyPath() {
        Product p = newProduct(30, "100.00", "150.00");
        SaleItem in = sItem(p, 5);
        in.setUnitPrice(BigDecimal.ONE); // tampered client price must be ignored
        Sale s = sales.create(null, List.of(in));
        assertEquals(25, stock(p));
        assertEquals(0, new BigDecimal("750.00").compareTo(s.getTotalAmount()));
        assertTrue(s.getInvoiceNo().startsWith("INV-"));
        assertEquals(1, sales.items(s).size());
        assertEquals(0, new BigDecimal("100.00").compareTo(sales.items(s).get(0).getUnitCost()));
    }

    @Test @DisplayName("21 sale: insufficient stock rejected with Available/Requested")
    void saleInsufficient() {
        Product p = newProduct(4, "10.00", "20.00");
        long before = count("sales");
        InsufficientStockException e = assertThrows(InsufficientStockException.class, () -> sales.create(null, List.of(sItem(p, 7))));
        assertEquals(4, e.getAvailable());
        assertEquals(7, e.getRequested());
        assertTrue(e.getMessage().contains("Available: 4") && e.getMessage().contains("Requested: 7"));
        assertEquals(4, stock(p));
        assertEquals(before, count("sales"));
    }

    @Test @DisplayName("22 sale: second line insufficient rolls back the first line")
    void saleRollback() {
        Product ok = newProduct(10, "10.00", "20.00");
        Product few = newProduct(1, "10.00", "20.00");
        long before = count("sales"), items = count("sale_items");
        assertThrows(InsufficientStockException.class, () -> sales.create(null, List.of(sItem(ok, 3), sItem(few, 2))));
        assertEquals(10, stock(ok));
        assertEquals(1, stock(few));
        assertEquals(before, count("sales"));
        assertEquals(items, count("sale_items"));
    }

    @Test @DisplayName("23 sale: DB failure after stock decrease rolls everything back")
    void saleRollbackOnDbFailure() throws Exception {
        Product ok = newProduct(10, "10.00", "20.00");
        exec("ALTER TABLE sale_items ADD CONSTRAINT chk_test_q7 CHECK (quantity <> 7)");
        try {
            long before = count("sales");
            assertThrows(DatabaseException.class, () -> sales.create(null, List.of(sItem(ok, 7))));
            assertEquals(10, stock(ok));
            assertEquals(before, count("sales"));
        } finally {
            exec("ALTER TABLE sale_items DROP CHECK chk_test_q7");
        }
    }

    @Test @DisplayName("24 sale: invalid quantity, inactive product, empty cart")
    void saleInvalid() {
        Product p = newProduct(10, "10.00", "20.00");
        assertThrows(ValidationException.class, () -> sales.create(null, List.of(sItem(p, 0))));
        assertThrows(ValidationException.class, () -> sales.create(null, List.of(sItem(p, -1))));
        assertThrows(ValidationException.class, () -> sales.create(null, List.of()));
        products.setActive(productDAO.findById(p.getId()), false);
        assertThrows(ValidationException.class, () -> sales.create(null, List.of(sItem(p, 1))));
        assertEquals(10, stock(p));
    }

    @Test @DisplayName("25 sale: two buyers race for the last unit - exactly one wins")
    void saleConcurrentLastUnit() throws Exception {
        Product p = newProduct(1, "10.00", "20.00");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) fs.add(pool.submit(() -> {
            go.await();
            try { sales.create(null, List.of(sItem(p, 1))); return true; } catch (InsufficientStockException e) { return false; }
        }));
        go.countDown();
        int wins = 0;
        for (Future<Boolean> f : fs) if (f.get(60, TimeUnit.SECONDS)) wins++;
        pool.shutdown();
        assertEquals(1, wins);
        assertEquals(0, stock(p));
    }

    @Test @DisplayName("26 sale: uses customer, walk-in has null customer")
    void saleCustomer() {
        Customer c = new Customer();
        c.setName("Cust " + run); c.setPhone("01912345678");
        customers.save(c);
        Product p = newProduct(10, "10.00", "20.00");
        sales.create(c, List.of(sItem(p, 1)));
        assertEquals(1, customers.history(c).size());
        assertEquals("Walk-in Customer", sales.create(null, List.of(sItem(p, 1))).getCustomerName());
        customers.setActive(c, false);
        assertThrows(ValidationException.class, () -> sales.create(c, List.of(sItem(p, 1))));
    }

    // ---------------------------------------------------------------- stale edit (lost update)
    @Test @DisplayName("30 product edit never undoes a purchase/sale made meanwhile")
    void staleEdit() {
        Product p = newProduct(10, "10.00", "20.00");
        Product formCopy = productDAO.findById(p.getId());       // form opened with stock 10
        purchases.create(supplier, List.of(pItem(p, 5, "10.00")), null); // stock is now 15
        formCopy.setLoadedQuantity(10);                           // user did not touch the quantity box
        formCopy.setName("Edited " + run);
        products.save(formCopy);
        assertEquals(15, stock(p), "stock must survive an edit that did not change the quantity");
        assertEquals("Edited " + run, productDAO.findById(p.getId()).getName());

        Product second = productDAO.findById(p.getId());
        second.setLoadedQuantity(15);
        sales.create(null, List.of(sItem(p, 1)));                 // stock 14 meanwhile
        second.setQuantity(100);                                  // user typed a new stock number from a stale view
        assertTrue(assertThrows(ValidationException.class, () -> products.save(second)).getMessage().contains("stock level changed"));
        assertEquals(14, stock(p));

        Product third = productDAO.findById(p.getId());
        third.setLoadedQuantity(14);
        third.setQuantity(20);                                    // deliberate correction, not stale
        products.save(third);
        assertEquals(20, stock(p));
    }

    // ---------------------------------------------------------------- session / roles
    @Test @DisplayName("40 users: cannot change own role; DB unchanged")
    void selfRoleChange() {
        User me = users.search("admin").stream().filter(u -> u.getUsername().equals("admin")).findFirst().orElseThrow();
        ValidationException e = assertThrows(ValidationException.class, () -> users.update(me, "Test Admin", Role.STAFF));
        assertTrue(e.getMessage().contains("own role"));
        assertEquals(Role.ADMIN, new com.stockflow.dao.UserDAO().findById(me.getId()).getRole());
        assertEquals(Role.ADMIN, Session.user().getRole());
        users.update(me, "Test Admin Renamed", Role.ADMIN); // renaming self is fine
        assertEquals("Test Admin Renamed", Session.user().getFullName());
        users.update(me, "Test Admin", Role.ADMIN);
        assertThrows(ValidationException.class, () -> users.setActive(me, false));
    }

    @Test @DisplayName("41 session: role changed elsewhere ends the session (UI and services agree)")
    void roleChangedElsewhere() throws Exception {
        String name = "rc" + run.toLowerCase();
        users.create(name, "Role Change", Role.MANAGER, "secret1", "secret1");
        auth.logout();
        auth.login(name, "secret1");
        assertTrue(Session.can(Permission.PURCHASES));
        Session.setRevalidateIntervalMillis(0);
        exec("UPDATE users SET role = 'STAFF' WHERE username = '" + name + "'");   // another admin demotes this user
        SessionExpiredException e = assertThrows(SessionExpiredException.class, () -> products.searchActive(""));
        assertTrue(e.getMessage().contains("role was changed"));
        assertFalse(Session.loggedIn());
        auth.login(name, "secret1");
        assertFalse(Session.can(Permission.PURCHASES), "after re-login the new (STAFF) role applies");
        assertThrows(PermissionException.class, () -> purchases.search("", null, null));
    }

    @Test @DisplayName("42 session: deactivated account cannot keep working")
    void deactivatedWhileLoggedIn() throws Exception {
        String name = "da" + run.toLowerCase();
        users.create(name, "Deact", Role.MANAGER, "secret1", "secret1");
        auth.logout();
        auth.login(name, "secret1");
        Session.setRevalidateIntervalMillis(0);
        exec("UPDATE users SET active = FALSE WHERE username = '" + name + "'");
        assertThrows(SessionExpiredException.class, () -> products.searchActive(""));
        assertFalse(Session.loggedIn());
        assertThrows(PermissionException.class, () -> products.searchActive(""));
    }

    @Test @DisplayName("43 users: editing a stale row cannot re-activate someone else deactivated")
    void staleUserRow() {
        String name = "st" + run.toLowerCase();
        users.create(name, "Stale", Role.STAFF, "secret1", "secret1");
        User staleRow = users.search(name).get(0);      // admin A's table row: active = true
        User other = users.search(name).get(0);         // admin B deactivates it meanwhile
        users.setActive(other, false);
        users.update(staleRow, "Stale Renamed", Role.STAFF);
        User now = users.search(name).get(0);
        assertEquals("Stale Renamed", now.getFullName());
        assertFalse(now.isActive(), "must stay deactivated");
    }

    @Test @DisplayName("44 users: last active admin is protected")
    void lastAdmin() {
        User admin = users.search("admin").stream().filter(u -> u.getUsername().equals("admin")).findFirst().orElseThrow();
        assertThrows(ValidationException.class, () -> users.setActive(admin, false));
    }

    @Test @DisplayName("45 roles: STAFF permissions")
    void staffRole() {
        auth.logout();
        auth.login("staff", TestDb.STAFF_PW);
        Product sp = new Product();
        sp.setSku("S-" + run); sp.setName("x"); sp.setCategoryId(category.getId());
        assertThrows(PermissionException.class, () -> products.save(sp));
        assertThrows(PermissionException.class, () -> purchases.search("", null, null));
        assertThrows(PermissionException.class, () -> users.search(""));
        assertThrows(PermissionException.class, () -> suppliers.search("", false));
        assertThrows(PermissionException.class, () -> dashboard.stats());
        assertThrows(PermissionException.class, () -> reports.inventory());
        assertThrows(PermissionException.class, () -> ActivityService.search(""));
        assertThrows(PermissionException.class, () -> categories.save(new Category(0, "nope")));
        Customer nc = new Customer();
        nc.setName("Walk " + run); nc.setPhone("01712345678");
        customers.save(nc);
        assertTrue(nc.getId() > 0);
        assertThrows(PermissionException.class, () -> customers.setActive(nc, false));
        Product p = newProductAsAdmin();
        assertTrue(sales.create(nc, List.of(sItem(p, 1))).getId() > 0);
        assertFalse(products.searchActive("").isEmpty());
        assertFalse(Session.can(Permission.USERS));
    }

    Product newProductAsAdmin() {
        Session.end();
        auth.login("admin", TestDb.ADMIN_PW);
        Product p = newProduct(5, "1.00", "2.00");
        Session.end();
        auth.login("staff", TestDb.STAFF_PW);
        return p;
    }

    @Test @DisplayName("46 roles: MANAGER permissions")
    void managerRole() {
        auth.logout();
        auth.login("manager", TestDb.MANAGER_PW);
        assertNotNull(dashboard.stats());
        assertFalse(reports.inventory().isEmpty());
        assertThrows(PermissionException.class, () -> users.search(""));
        assertThrows(PermissionException.class, () -> ActivityService.search(""));
        Product p = newProduct(3, "1.00", "2.00");
        products.setActive(productDAO.findById(p.getId()), false);
        assertFalse(productDAO.findById(p.getId()).isActive());
        assertFalse(purchases.search("", null, null) == null);
    }

    // ---------------------------------------------------------------- validation of contacts
    @Test @DisplayName("50 customers/suppliers: phone and email validation")
    void contactValidation() {
        Customer c = new Customer();
        c.setName("Bad"); c.setPhone("abc");
        assertThrows(ValidationException.class, () -> customers.save(c));
        c.setPhone("01712345678"); c.setEmail("not-an-email");
        assertThrows(ValidationException.class, () -> customers.save(c));
        c.setEmail(""); customers.save(c);
        assertTrue(c.getId() > 0);
        Supplier s = new Supplier();
        s.setName(" "); s.setPhone("01712345678");
        assertThrows(ValidationException.class, () -> suppliers.save(s));
    }

    // ---------------------------------------------------------------- dashboard / reports
    @Test @DisplayName("60 dashboard and reports match the database")
    void dashboardAndReports() {
        Product p = newProduct(50, "40.00", "100.00");
        purchases.create(supplier, List.of(pItem(p, 10, "40.00")), null);
        sales.create(null, List.of(sItem(p, 3)));
        DashboardStats s = dashboard.stats();
        BigDecimal revenue = Jdbc.one("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE DATE(sale_date) = CURDATE()", rs -> rs.getBigDecimal(1));
        BigDecimal purchased = Jdbc.one("SELECT COALESCE(SUM(total_amount),0) FROM purchases WHERE DATE(purchase_date) = CURDATE()", rs -> rs.getBigDecimal(1));
        BigDecimal profit = Jdbc.one("SELECT COALESCE(SUM((unit_price-unit_cost)*quantity),0) FROM sale_items si JOIN sales s ON s.id=si.sale_id WHERE DATE(s.sale_date)=CURDATE()", rs -> rs.getBigDecimal(1));
        assertEquals(0, revenue.compareTo(s.todayRevenue()));
        assertEquals(0, purchased.compareTo(s.todayPurchases()));
        assertEquals(0, profit.compareTo(s.todayProfit()));
        long active = Jdbc.one("SELECT COUNT(*) FROM products WHERE active = TRUE", rs -> rs.getLong(1));
        long low = Jdbc.one("SELECT COUNT(*) FROM products WHERE active = TRUE AND quantity <= reorder_level", rs -> rs.getLong(1));
        BigDecimal value = Jdbc.one("SELECT COALESCE(SUM(cost_price*quantity),0) FROM products WHERE active = TRUE", rs -> rs.getBigDecimal(1));
        assertEquals(active, s.totalProducts());
        assertEquals(low, s.lowStock());
        assertEquals(0, value.compareTo(s.stockValue()));
        assertEquals(7, dashboard.last7Days().size());
        assertEquals(0, revenue.compareTo(dashboard.last7Days().get(6).revenue()));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Dhaka"));
        SalesSummary todaySum = reports.sales(today, today);
        assertEquals(0, revenue.compareTo(todaySum.revenue()));
        assertEquals(0, profit.compareTo(todaySum.profit()));
        SalesSummary yesterday = reports.sales(today.minusDays(1), today.minusDays(1));
        assertEquals(0, yesterday.count());
        assertEquals(0, BigDecimal.ZERO.compareTo(yesterday.revenue()));
        assertTrue(reports.topProducts(today, today).stream().anyMatch(t -> t.name().equals(productDAO.findById(p.getId()).getName()) && t.quantity() == 3));
        assertTrue(reports.purchasesBySupplier(today, today).stream().anyMatch(t -> t.supplier().equals(supplier.getName())));
        assertTrue(reports.purchasesBySupplier(today.plusDays(1), today.plusDays(2)).isEmpty());
        LocalDate[] week = reports.resolve(ReportService.Range.THIS_WEEK, null, null);
        assertEquals(java.time.DayOfWeek.SATURDAY, week[0].getDayOfWeek());
        assertThrows(ValidationException.class, () -> reports.resolve(ReportService.Range.CUSTOM, today, today.minusDays(1)));
    }

    @Test @DisplayName("61 activity log records key actions")
    void activityLog() {
        Product p = newProduct(1, "1.00", "2.00");
        sales.create(null, List.of(sItem(p, 1)));
        List<ActivityLog> logs = ActivityService.search("");
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("Product added") && l.getDetails().contains(p.getSku())));
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("Sale created")));
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("User logged in")));
    }

    @Test @DisplayName("62 database session time zone is Bangladesh time")
    void timeZone() {
        LocalDateTime db = Jdbc.one("SELECT NOW()", rs -> rs.getTimestamp(1).toLocalDateTime());
        LocalDateTime dhaka = LocalDateTime.now(ZoneId.of("Asia/Dhaka"));
        assertTrue(Math.abs(Duration.between(db, dhaka).getSeconds()) < 10, "DB says " + db + " but Dhaka clock says " + dhaka);
    }
}
