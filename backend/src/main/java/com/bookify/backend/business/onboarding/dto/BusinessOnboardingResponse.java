package com.bookify.backend.business.onboarding.dto;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.tenancy.model.MembershipRole;

public record BusinessOnboardingResponse(
        Long id,
        String name,
        String slug,
        String categoryCode,
        MembershipRole membershipRole,
        LocationResponse location
) {
    public static BusinessOnboardingResponse from(
            Business business,
            MembershipRole membershipRole,
            BusinessLocation location
    ) {
        return new BusinessOnboardingResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                business.getCategoryCode(),
                membershipRole,
                LocationResponse.from(location)
        );
    }
}
