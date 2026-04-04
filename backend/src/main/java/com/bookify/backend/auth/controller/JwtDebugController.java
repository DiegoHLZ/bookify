package com.bookify.backend.auth.controller;

import com.bookify.backend.auth.service.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class JwtDebugController {

    private final JwtService jwtService;

    public JwtDebugController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/api/test/token-info")
    public Map<String, Object> tokenInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);

        Map<String, Object> response = new HashMap<>();
        response.put("email", jwtService.extractUsername(token));
        response.put("role", jwtService.extractRole(token));
        response.put("businessId", jwtService.extractBusinessId(token));

        return response;
    }
}
