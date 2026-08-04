package com.bookify.backend.tenancy.repository;

import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, Long> {

    boolean existsByBusinessIdAndUserEmailAndActiveTrue(Long businessId, String email);

    boolean existsByBusinessIdAndUserEmailAndRoleInAndActiveTrue(
            Long businessId,
            String email,
            Collection<MembershipRole> roles
    );

    @Query("""
            select membership
            from BusinessMembership membership
            join fetch membership.business business
            where lower(membership.user.email) = lower(:email)
              and membership.active = true
              and business.active = true
            order by business.name asc
            """)
    List<BusinessMembership> findActiveBusinessesByUserEmail(@Param("email") String email);

    @Query("""
            select membership
            from BusinessMembership membership
            join fetch membership.user user
            where membership.business.id = :businessId
            order by membership.active desc, lower(user.email) asc, membership.id asc
            """)
    List<BusinessMembership> findMembers(@Param("businessId") Long businessId);

    Optional<BusinessMembership> findByIdAndBusinessId(Long id, Long businessId);

    Optional<BusinessMembership> findByBusinessIdAndUserEmailIgnoreCase(
            Long businessId,
            String email
    );

    long countByBusinessIdAndRoleAndActiveTrue(Long businessId, MembershipRole role);
}
