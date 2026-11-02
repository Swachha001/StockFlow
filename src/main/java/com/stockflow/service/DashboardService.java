package com.stockflow.service;

import com.stockflow.dao.ProductDAO;
import com.stockflow.dao.ReportDAO;
import com.stockflow.model.Permission;
import com.stockflow.model.Product;
import com.stockflow.model.ReportModels.*;
import com.stockflow.util.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final ReportDAO reportDAO = new ReportDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public DashboardStats stats() {
        Session.require(Permission.DASHBOARD);
        return reportDAO.dashboard();
    }

    /** Always returns 7 entries (today and the 6 days before), filling missing days with zero. */
    public List<DailySales> last7Days() {
        Session.require(Permission.DASHBOARD);
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (DailySales d : reportDAO.salesLast7Days()) byDay.put(d.date(), d.revenue());
        List<DailySales> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            out.add(new DailySales(d, byDay.getOrDefault(d, BigDecimal.ZERO)));
        }
        return out;
    }

    public List<Product> lowStock() {
        Session.require(Permission.DASHBOARD);
        return productDAO.findLowStock();
    }

    public List<Txn> recent() {
        Session.require(Permission.DASHBOARD);
        return reportDAO.recentTransactions(10);
    }
}
