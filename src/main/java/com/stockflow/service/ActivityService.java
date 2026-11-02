package com.stockflow.service;

import com.stockflow.dao.ActivityLogDAO;
import com.stockflow.model.ActivityLog;
import com.stockflow.model.Permission;
import com.stockflow.util.Session;

import java.util.List;

public final class ActivityService {
    private static final ActivityLogDAO DAO = new ActivityLogDAO();

    private ActivityService() { }

    /** Never throws: a logging failure must not break the business operation. */
    public static void log(String action, String details) {
        try {
            DAO.insert(Session.userId(), action, details);
        } catch (RuntimeException e) {
            System.err.println("Could not write activity log: " + e.getMessage());
        }
    }

    public static List<ActivityLog> search(String q) {
        Session.require(Permission.LOGS);
        return DAO.search(q);
    }
}
