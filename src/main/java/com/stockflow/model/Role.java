package com.stockflow.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    MANAGER(EnumSet.complementOf(EnumSet.of(Permission.USERS, Permission.LOGS))),
    STAFF(EnumSet.of(Permission.PRODUCTS_VIEW, Permission.CUSTOMERS_VIEW, Permission.CUSTOMERS_ADD, Permission.SALES));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean has(Permission p) {
        return permissions.contains(p);
    }
}
