package com.stockflow.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only result rows used by the dashboard and reports. */
public final class ReportModels {
    private ReportModels() { }

    public record DashboardStats(long totalProducts, long totalUnits, long lowStock, BigDecimal stockValue,
                                 long todaySalesCount, BigDecimal todayRevenue, BigDecimal todayPurchases,
                                 BigDecimal todayProfit) { }

    public record DailySales(LocalDate date, BigDecimal revenue) { }

    public record Txn(String type, String ref, String party, BigDecimal amount, LocalDateTime when) { }

    public record TopProduct(String name, long quantity, BigDecimal revenue) { }

    public record SupplierTotal(String supplier, long purchases, BigDecimal total) { }

    public record HistoryRow(String ref, LocalDateTime date, BigDecimal amount, String extra) { }

    public record SalesSummary(long count, BigDecimal revenue, BigDecimal cost) {
        public BigDecimal profit() { return revenue.subtract(cost); }

        public BigDecimal marginPercent() {
            if (revenue.signum() == 0) return BigDecimal.ZERO;
            return profit().multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);
        }
    }
}
