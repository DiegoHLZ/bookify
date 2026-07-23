package com.bookify.backend.config;

public class AuthenticatedUser {

    private final String email;
    private final String role;

    public AuthenticatedUser(String email, String role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

}
