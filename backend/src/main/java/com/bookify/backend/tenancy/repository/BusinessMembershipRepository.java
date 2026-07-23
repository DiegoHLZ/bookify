package com.bookify.backend.tenancy.repository;

import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, Long> {

    boolean existsByBusinessIdAndUserEmailAndActiveTrue(Long businessId, String email);

    boolean existsByBusinessIdAndUserEmailAndRoleInAndActiveTrue(
            Long businessId,
            String email,
            Collection<MembershipRole> roles
    );
}
