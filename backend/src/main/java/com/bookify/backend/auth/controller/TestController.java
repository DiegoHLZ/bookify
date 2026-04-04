package com.bookify.backend.auth.controller;

import com.bookify.backend.common.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello secured endpoint";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminOnly() {
        return "Hello admin endpoint";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @GetMapping("/user")
    public String userOrAdmin() {
        return "Hello user or admin endpoint";
    }

    @GetMapping("/me")
    public String me() {
        return SecurityUtils.getCurrentUserEmail();
    }

    @GetMapping("/me-role")
    public String role(Authentication authentication) {
        return authentication.getAuthorities().toString();
    }
}
