// src/main/java/com/library/model/UserRole.java
package com.library.model;

public enum UserRole {
    ROLE_ADMIN,
    ROLE_LIBRARIAN,
    ROLE_ASSISTANT,
    ROLE_USER;

    // Optional helper methods
    public String getAuthority() {
        return name(); // returns "ROLE_ADMIN" etc.
    }

    public static UserRole fromString(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No enum constant " + UserRole.class.getCanonicalName() + "." + role);
        }
    }
}