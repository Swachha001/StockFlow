package com.stockflow.service;

import com.stockflow.dao.UserDAO;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.Permission;
import com.stockflow.model.Role;
import com.stockflow.model.User;
import com.stockflow.util.Check;
import com.stockflow.util.Session;

import java.util.List;

public class UserService {
    private final UserDAO dao = new UserDAO();

    public List<User> search(String q) {
        Session.require(Permission.USERS);
        return dao.search(q);
    }

    public void create(String username, String fullName, Role role, String password, String confirm) {
        Session.require(Permission.USERS);
        String u = Check.required(username, "Username");
        if (!u.matches("[A-Za-z0-9_.]{3,50}")) throw new ValidationException("Username must be 3-50 letters, digits, '.' or '_'.");
        String name = Check.maxLen(Check.required(fullName, "Full name"), 100, "Full name");
        if (role == null) throw new ValidationException("Role is required.");
        AuthService.validateNewPassword(password, confirm);
        if (dao.usernameExists(u)) throw new ValidationException("Username already exists.");
        User user = new User();
        user.setUsername(u);
        user.setFullName(name);
        user.setRole(role);
        user.setPasswordHash(AuthService.hash(password));
        dao.insert(user);
        ActivityService.log("User created", u + " (" + role + ")");
    }

    public void update(User user, String fullName, Role role) {
        Session.require(Permission.USERS);
        String name = Check.maxLen(Check.required(fullName, "Full name"), 100, "Full name");
        if (role == null) throw new ValidationException("Role is required.");
        User fresh = dao.findById(user.getId()); // never trust the (possibly stale) table row
        if (fresh == null) throw new ValidationException("This user no longer exists.");
        boolean self = Session.user().getId() == fresh.getId();
        if (self && role != fresh.getRole()) {
            throw new ValidationException("You cannot change your own role. Ask another administrator to do it.");
        }
        if (fresh.getRole() == Role.ADMIN && role != Role.ADMIN && fresh.isActive() && dao.countActiveAdmins() <= 1) {
            throw new ValidationException("You cannot change the role of the last active administrator.");
        }
        dao.updateProfile(fresh.getId(), name, role);
        user.setFullName(name);
        user.setRole(role);
        user.setActive(fresh.isActive());
        ActivityService.log("User updated", fresh.getUsername() + " (" + role + ")");
        if (self) Session.revalidate(); // refresh the logged-in user's details
    }

    public void resetPassword(User user, String password, String confirm) {
        Session.require(Permission.USERS);
        AuthService.validateNewPassword(password, confirm);
        dao.updatePassword(user.getId(), AuthService.hash(password));
        ActivityService.log("Password reset", user.getUsername());
    }

    public void setActive(User user, boolean active) {
        Session.require(Permission.USERS);
        User fresh = dao.findById(user.getId());
        if (fresh == null) throw new ValidationException("This user no longer exists.");
        if (!active) {
            if (Session.user().getId() == fresh.getId()) throw new ValidationException("You cannot deactivate your own account.");
            if (fresh.getRole() == Role.ADMIN && fresh.isActive() && dao.countActiveAdmins() <= 1) {
                throw new ValidationException("You cannot deactivate the last active administrator.");
            }
        }
        dao.setActive(fresh.getId(), active);
        user.setActive(active);
        ActivityService.log(active ? "User activated" : "User deactivated", fresh.getUsername());
    }
}
