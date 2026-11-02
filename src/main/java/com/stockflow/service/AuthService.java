package com.stockflow.service;

import com.stockflow.dao.UserDAO;
import com.stockflow.exception.AppException;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.User;
import com.stockflow.util.Session;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    static {
        // lets Session re-check the account (active? same role?) against the database
        Session.setRefresher(id -> new UserDAO().findById(id));
    }

    public User login(String username, String password) {
        User user = userDAO.findByUsername(username == null ? "" : username.trim());
        if (user == null || !passwordMatches(password, user.getPasswordHash())) {
            throw new AppException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new AppException("This account is inactive. Please contact an administrator.");
        }
        Session.start(user);
        ActivityService.log("User logged in", user.getUsername());
        return user;
    }

    public void logout() {
        if (Session.loggedIn()) ActivityService.log("User logged out", Session.user().getUsername());
        Session.end();
    }

    public void changePassword(String oldPassword, String newPassword, String confirm) {
        User me = Session.user();
        if (me == null) throw new AppException("Not logged in.");
        User fresh = userDAO.findById(me.getId());
        if (!passwordMatches(oldPassword, fresh.getPasswordHash())) throw new ValidationException("Current password is incorrect.");
        validateNewPassword(newPassword, confirm);
        userDAO.updatePassword(me.getId(), hash(newPassword));
        ActivityService.log("Password changed", me.getUsername());
    }

    public static void validateNewPassword(String password, String confirm) {
        if (password == null || password.length() < 6) throw new ValidationException("New password must be at least 6 characters.");
        if (!password.equals(confirm)) throw new ValidationException("Passwords do not match.");
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    private static boolean passwordMatches(String password, String hash) {
        try {
            return password != null && hash != null && BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
