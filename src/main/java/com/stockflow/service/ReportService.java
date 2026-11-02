package com.stockflow.service;

import com.stockflow.dao.ProductDAO;
import com.stockflow.dao.ReportDAO;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.Permission;
import com.stockflow.model.Product;
import com.stockflow.model.ReportModels.*;
import com.stockflow.util.Session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReportDAO dao = new ReportDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public enum Range {
        TODAY("Today"), THIS_WEEK("This Week"), THIS_MONTH("This Month"), CUSTOM("Custom Range");

        private final String label;

        Range(String label) { this.label = label; }

        @Override public String toString() { return label; }
    }

    /** Returns {from, to} for the chosen range (week starts on Saturday, as is common in Bangladesh). */
    public LocalDate[] resolve(Range range, LocalDate customFrom, LocalDate customTo) {
        LocalDate today = LocalDate.now();
        switch (range) {
            case TODAY:
                return new LocalDate[]{today, today};
            case THIS_WEEK: {
                LocalDate start = today;
                while (start.getDayOfWeek() != DayOfWeek.SATURDAY) start = start.minusDays(1);
                return new LocalDate[]{start, today};
            }
            case THIS_MONTH:
                return new LocalDate[]{today.withDayOfMonth(1), today};
            default:
                if (customFrom == null || customTo == null) throw new ValidationException("Choose both From and To dates.");
                if (customFrom.isAfter(customTo)) throw new ValidationException("'From' date must not be after 'To' date.");
                return new LocalDate[]{customFrom, customTo};
        }
    }

    public SalesSummary sales(LocalDate from, LocalDate to) {
        Session.require(Permission.REPORTS);
        return dao.salesSummary(from, to);
    }

    public List<TopProduct> topProducts(LocalDate from, LocalDate to) {
        Session.require(Permission.REPORTS);
        return dao.topProducts(from, to, 10);
    }

    public List<SupplierTotal> purchasesBySupplier(LocalDate from, LocalDate to) {
        Session.require(Permission.REPORTS);
        return dao.purchasesBySupplier(from, to);
    }

    public List<Product> inventory() {
        Session.require(Permission.REPORTS);
        return productDAO.search("", null, false, false);
    }
}
