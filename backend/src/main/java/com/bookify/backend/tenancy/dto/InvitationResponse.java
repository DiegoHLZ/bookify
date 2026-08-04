package com.bookify.backend.tenancy.dto;

import com.bookify.backend.tenancy.model.InvitationStatus;
import com.bookify.backend.tenancy.model.MembershipRole;

import java.time.Instant;

public record InvitationResponse(
        Long id,
        Long businessId,
        String email,
        MembershipRole role,
        InvitationStatus status,
        Instant expiresAt,
        String invitedByEmail,
        Instant createdAt
) {
}
