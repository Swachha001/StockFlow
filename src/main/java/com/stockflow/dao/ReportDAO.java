package com.stockflow.dao;

import com.stockflow.model.ReportModels.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDAO {

    private static BigDecimal money(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        BigDecimal v = rs.getBigDecimal(i);
        return v == null ? BigDecimal.ZERO : v;
    }

    public DashboardStats dashboard() {
        long[] inv = new long[3];
        BigDecimal[] value = {BigDecimal.ZERO};
        Jdbc.one("SELECT COUNT(*), COALESCE(SUM(quantity),0), COALESCE(SUM(CASE WHEN quantity <= reorder_level THEN 1 ELSE 0 END),0), "
                + "COALESCE(SUM(cost_price * quantity),0) FROM products WHERE active = TRUE", rs -> {
            inv[0] = rs.getLong(1);
            inv[1] = rs.getLong(2);
            inv[2] = rs.getLong(3);
            value[0] = money(rs, 4);
            return true;
        });
        long[] salesCount = {0};
        BigDecimal[] revenue = {BigDecimal.ZERO};
        Jdbc.one("SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM sales WHERE DATE(sale_date) = CURDATE()", rs -> {
            salesCount[0] = rs.getLong(1);
            revenue[0] = money(rs, 2);
            return true;
        });
        BigDecimal purchases = Jdbc.one("SELECT COALESCE(SUM(total_amount),0) FROM purchases WHERE DATE(purchase_date) = CURDATE()", rs -> money(rs, 1));
        BigDecimal profit = Jdbc.one("SELECT COALESCE(SUM(si.subtotal - si.unit_cost * si.quantity),0) FROM sale_items si "
                + "JOIN sales s ON s.id = si.sale_id WHERE DATE(s.sale_date) = CURDATE()", rs -> money(rs, 1));
        return new DashboardStats(inv[0], inv[1], inv[2], value[0], salesCount[0], revenue[0], purchases, profit);
    }

    public List<DailySales> salesLast7Days() {
        return Jdbc.query("SELECT DATE(sale_date) AS d, SUM(total_amount) FROM sales WHERE DATE(sale_date) >= CURDATE() - INTERVAL 6 DAY "
                + "GROUP BY DATE(sale_date) ORDER BY d", rs -> new DailySales(rs.getDate(1).toLocalDate(), money(rs, 2)));
    }

    public List<Txn> recentTransactions(int limit) {
        return Jdbc.query("(SELECT 'Sale' AS type, s.invoice_no AS ref, COALESCE(c.name, 'Walk-in Customer') AS party, s.total_amount AS amount, s.sale_date AS txn_time "
                + "FROM sales s LEFT JOIN customers c ON c.id = s.customer_id) UNION ALL "
                + "(SELECT 'Purchase', CONCAT('PUR-', LPAD(pu.id, 4, '0')), sp.name, pu.total_amount, pu.purchase_date "
                + "FROM purchases pu JOIN suppliers sp ON sp.id = pu.supplier_id) ORDER BY txn_time DESC LIMIT ?",
                rs -> new Txn(rs.getString(1), rs.getString(2), rs.getString(3), money(rs, 4), Jdbc.ts(rs, "txn_time")), limit);
    }

    public SalesSummary salesSummary(LocalDate from, LocalDate to) {
        long[] count = {0};
        BigDecimal[] revenue = {BigDecimal.ZERO};
        Jdbc.one("SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM sales WHERE DATE(sale_date) BETWEEN ? AND ?", rs -> {
            count[0] = rs.getLong(1);
            revenue[0] = money(rs, 2);
            return true;
        }, from, to);
        BigDecimal cost = Jdbc.one("SELECT COALESCE(SUM(si.unit_cost * si.quantity),0) FROM sale_items si JOIN sales s ON s.id = si.sale_id "
                + "WHERE DATE(s.sale_date) BETWEEN ? AND ?", rs -> money(rs, 1), from, to);
        return new SalesSummary(count[0], revenue[0], cost);
    }

    public List<TopProduct> topProducts(LocalDate from, LocalDate to, int limit) {
        return Jdbc.query("SELECT p.name, SUM(si.quantity) AS qty, SUM(si.subtotal) AS rev FROM sale_items si "
                + "JOIN sales s ON s.id = si.sale_id JOIN products p ON p.id = si.product_id "
                + "WHERE DATE(s.sale_date) BETWEEN ? AND ? GROUP BY p.id, p.name ORDER BY qty DESC, rev DESC LIMIT ?",
                rs -> new TopProduct(rs.getString(1), rs.getLong(2), money(rs, 3)), from, to, limit);
    }

    public List<SupplierTotal> purchasesBySupplier(LocalDate from, LocalDate to) {
        return Jdbc.query("SELECT sp.name, COUNT(*), SUM(pu.total_amount) AS total FROM purchases pu JOIN suppliers sp ON sp.id = pu.supplier_id "
                + "WHERE DATE(pu.purchase_date) BETWEEN ? AND ? GROUP BY sp.id, sp.name ORDER BY total DESC",
                rs -> new SupplierTotal(rs.getString(1), rs.getLong(2), money(rs, 3)), from, to);
    }
}
