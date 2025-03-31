package com.library.cli;

public enum Role {
    ROLE_ADMIN,     // Add this
    ROLE_LIBRARIAN, // Add this
    ROLE_ASSISTANT, // Add this
    ROLE_USER;      // Keep existing if present

    public static Role fromString(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No enum constant " + Role.class.getCanonicalName() + "." + role);
        }
    }
}