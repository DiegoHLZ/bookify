package com.bookify.backend.tenancy.service;

import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.model.MembershipPermission;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class BusinessAccessService {

    private static final EnumSet<MembershipRole> MANAGEMENT_ROLES =
            EnumSet.of(MembershipRole.OWNER, MembershipRole.ADMIN);

    private final BusinessMembershipRepository membershipRepository;

    public BusinessAccessService(BusinessMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public void requireMembership(Long businessId, String email) {
        if (!membershipRepository.existsByBusinessIdAndUserEmailAndActiveTrue(businessId, email)) {
            throw new AccessDeniedException("Business membership required");
        }
    }

    public void requireManagementAccess(Long businessId, String email) {
        if (!membershipRepository.existsByBusinessIdAndUserEmailAndRoleInAndActiveTrue(
                businessId,
                email,
                MANAGEMENT_ROLES
        )) {
            throw new AccessDeniedException("Business management access required");
        }
    }

    public MembershipRole requireRole(Long businessId, String email) {
        return membershipRepository.findByBusinessIdAndUserEmailIgnoreCase(businessId, email)
                .filter(com.bookify.backend.tenancy.model.BusinessMembership::isActive)
                .map(com.bookify.backend.tenancy.model.BusinessMembership::getRole)
                .orElseThrow(() -> new AccessDeniedException("Business membership required"));
    }

    public Set<MembershipPermission> permissionsFor(MembershipRole role) {
        return switch (role) {
            case OWNER -> EnumSet.allOf(MembershipPermission.class);
            case ADMIN -> EnumSet.of(
                    MembershipPermission.VIEW_BUSINESS,
                    MembershipPermission.MANAGE_BUSINESS,
                    MembershipPermission.MANAGE_CATALOG,
                    MembershipPermission.MANAGE_SCHEDULES,
                    MembershipPermission.MANAGE_BOOKINGS,
                    MembershipPermission.VIEW_MEMBERS,
                    MembershipPermission.INVITE_STAFF,
                    MembershipPermission.MANAGE_STAFF
            );
            case STAFF -> EnumSet.of(
                    MembershipPermission.VIEW_BUSINESS,
                    MembershipPermission.MANAGE_BOOKINGS
            );
        };
    }
}
