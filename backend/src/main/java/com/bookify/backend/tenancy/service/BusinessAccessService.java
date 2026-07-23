package com.bookify.backend.tenancy.service;

import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

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
}
