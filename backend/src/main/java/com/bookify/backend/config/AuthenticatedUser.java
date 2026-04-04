package com.bookify.backend.config;

public class AuthenticatedUser {

    private final String email;
    private final String role;
    private final Long businessId;

    public AuthenticatedUser(String email, String role, Long businessId) {
        this.email = email;
        this.role = role;
        this.businessId = businessId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getBusinessId() {
        return businessId;
    }
}
