package com.bookify.backend.tenancy.dto;

import com.bookify.backend.tenancy.model.MembershipRole;

import java.time.LocalDateTime;

public record MemberResponse(
        Long membershipId,
        Long userId,
        String firstName,
        String lastName,
        String email,
        MembershipRole role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
