package com.bookify.backend.tenancy.dto;

import com.bookify.backend.tenancy.model.MembershipPermission;
import com.bookify.backend.tenancy.model.MembershipRole;

import java.util.Set;

public record MembershipPermissionsResponse(
        Long businessId,
        MembershipRole role,
        Set<MembershipPermission> permissions
) {
}
