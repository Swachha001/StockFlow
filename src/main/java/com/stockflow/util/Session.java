package com.stockflow.util;

import com.stockflow.exception.PermissionException;
import com.stockflow.exception.SessionExpiredException;
import com.stockflow.model.Permission;
import com.stockflow.model.User;

import java.util.function.Function;

/**
 * The currently logged-in user. Permission checks use the in-memory role, but the account is re-read from the
 * database at most every {@code revalidateIntervalMillis} (and by the main window's timer), so a deactivated
 * account or a changed role cannot keep working in an old session: the session is ended and the user must log in again.
 */
public final class Session {
    private static User current;
    private static long lastCheck;
    private static long revalidateIntervalMillis = 30_000;
    private static Function<Integer, User> refresher;

    private Session() { }

    public static synchronized void start(User user) {
        current = user;
        lastCheck = System.currentTimeMillis();
    }

    public static synchronized void end() { current = null; }
    public static synchronized User user() { return current; }
    public static synchronized boolean loggedIn() { return current != null; }
    public static synchronized Integer userId() { return current == null ? null : current.getId(); }

    /** Registers how to reload a user by id (set by AuthService). */
    public static synchronized void setRefresher(Function<Integer, User> loader) { refresher = loader; }

    public static synchronized void setRevalidateIntervalMillis(long millis) { revalidateIntervalMillis = millis; }

    public static synchronized boolean can(Permission p) {
        return current != null && current.getRole().has(p);
    }

    /** Throws if the logged-in account was deactivated/removed or its role changed since login. */
    public static synchronized void revalidate() {
        if (current == null || refresher == null) return;
        User fresh = refresher.apply(current.getId());
        lastCheck = System.currentTimeMillis();
        if (fresh == null || !fresh.isActive()) {
            current = null;
            throw new SessionExpiredException("Your account is no longer active. Please contact an administrator.");
        }
        if (fresh.getRole() != current.getRole()) {
            com.stockflow.model.Role newRole = fresh.getRole();
            current = null;
            throw new SessionExpiredException("Your role was changed to " + newRole + ". Please log in again.");
        }
        current = fresh; // keep name etc. up to date
    }

    private static synchronized void revalidateIfDue() {
        if (current != null && System.currentTimeMillis() - lastCheck >= revalidateIntervalMillis) revalidate();
    }

    public static void require(Permission p) {
        revalidateIfDue();
        if (!can(p)) throw new PermissionException("You do not have permission to perform this action.");
    }
}
