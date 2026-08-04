package com.bookify.backend.tenancy.dto;

import com.bookify.backend.tenancy.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvitationRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull MembershipRole role
) {
}
