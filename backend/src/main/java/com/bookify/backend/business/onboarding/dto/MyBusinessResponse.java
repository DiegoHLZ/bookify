package com.bookify.backend.business.onboarding.dto;

import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;

public record MyBusinessResponse(
        Long id,
        String name,
        String slug,
        String categoryCode,
        MembershipRole membershipRole
) {
    public static MyBusinessResponse from(BusinessMembership membership) {
        return new MyBusinessResponse(
                membership.getBusiness().getId(),
                membership.getBusiness().getName(),
                membership.getBusiness().getSlug(),
                membership.getBusiness().getCategoryCode(),
                membership.getRole()
        );
    }
}
