package com.bookify.backend.tenancy.dto;

import com.bookify.backend.tenancy.model.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(@NotNull MembershipRole role) {
}
