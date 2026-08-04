package com.bookify.backend.tenancy;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.tenancy.dto.CreateInvitationRequest;
import com.bookify.backend.tenancy.model.*;
import com.bookify.backend.tenancy.repository.BusinessInvitationRepository;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.tenancy.service.BusinessTeamService;
import com.bookify.backend.user.model.Role;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BusinessTeamServiceIntegrationTest {
    @Autowired
    private BusinessTeamService teamService;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BusinessMembershipRepository membershipRepository;
    @Autowired
    private BusinessInvitationRepository invitationRepository;

    private Business business;
    private User owner;
    private User admin;
    private User invitee;

    @BeforeEach
    void setUp() {
        business = businessRepository.save(business("team-business"));
        owner = userRepository.save(user("owner-team@bookify.test"));
        admin = userRepository.save(user("admin-team@bookify.test"));
        invitee = userRepository.save(user("invitee-team@bookify.test"));
        membershipRepository.save(
                new BusinessMembership(business, owner, MembershipRole.OWNER)
        );
        membershipRepository.save(
                new BusinessMembership(business, admin, MembershipRole.ADMIN)
        );
    }

    @Test
    void invitationTokenIsHashedAndAcceptanceCreatesMembership() {
        var created = teamService.invite(
                business.getId(),
                new CreateInvitationRequest(invitee.getEmail(), MembershipRole.STAFF),
                owner.getEmail()
        );

        BusinessInvitation stored = invitationRepository.findById(
                created.invitation().id()
        ).orElseThrow();
        assertNotEquals(created.invitationToken(), stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());
        assertEquals(InvitationStatus.PENDING, stored.getStatus());

        var member = teamService.acceptInvitation(
                created.invitationToken(), invitee.getEmail()
        );

        assertEquals(invitee.getEmail(), member.email());
        assertEquals(MembershipRole.STAFF, member.role());
        assertTrue(member.active());
        assertEquals(
                InvitationStatus.ACCEPTED,
                invitationRepository.findById(created.invitation().id())
                        .orElseThrow().getStatus()
        );
    }

    @Test
    void invitationCanOnlyBeAcceptedByMatchingUser() {
        var created = teamService.invite(
                business.getId(),
                new CreateInvitationRequest(invitee.getEmail(), MembershipRole.STAFF),
                owner.getEmail()
        );

        assertThrows(AccessDeniedException.class, () ->
                teamService.acceptInvitation(created.invitationToken(), admin.getEmail())
        );
        assertTrue(membershipRepository
                .findByBusinessIdAndUserEmailIgnoreCase(
                        business.getId(), invitee.getEmail()
                ).isEmpty());
    }

    @Test
    void adminCanInviteStaffButCannotAssignPrivilegedRoles() {
        assertDoesNotThrow(() -> teamService.invite(
                business.getId(),
                new CreateInvitationRequest(invitee.getEmail(), MembershipRole.STAFF),
                admin.getEmail()
        ));

        User second = userRepository.save(user("second-team@bookify.test"));
        assertThrows(AccessDeniedException.class, () -> teamService.invite(
                business.getId(),
                new CreateInvitationRequest(second.getEmail(), MembershipRole.ADMIN),
                admin.getEmail()
        ));
    }

    @Test
    void businessMustRetainAnActiveOwner() {
        BusinessMembership ownerMembership = membershipRepository
                .findByBusinessIdAndUserEmailIgnoreCase(
                        business.getId(), owner.getEmail()
                ).orElseThrow();

        assertThrows(BadRequestException.class, () -> teamService.changeStatus(
                business.getId(), ownerMembership.getId(), false, owner.getEmail()
        ));
        assertThrows(BadRequestException.class, () -> teamService.changeRole(
                business.getId(), ownerMembership.getId(), MembershipRole.ADMIN,
                owner.getEmail()
        ));
    }

    @Test
    void adminCannotManageOwnersOrOtherAdmins() {
        BusinessMembership ownerMembership = membershipRepository
                .findByBusinessIdAndUserEmailIgnoreCase(
                        business.getId(), owner.getEmail()
                ).orElseThrow();

        assertThrows(AccessDeniedException.class, () -> teamService.changeStatus(
                business.getId(), ownerMembership.getId(), false, admin.getEmail()
        ));
    }

    @Test
    void crossTenantMemberChangesAreRejected() {
        Business other = businessRepository.save(business("other-team-business"));
        User otherOwner = userRepository.save(user("other-owner@bookify.test"));
        BusinessMembership otherMembership = membershipRepository.save(
                new BusinessMembership(other, otherOwner, MembershipRole.OWNER)
        );

        assertThrows(AccessDeniedException.class, () -> teamService.changeStatus(
                other.getId(), otherMembership.getId(), false, owner.getEmail()
        ));
    }

    @Test
    void permissionsReflectOperationalRole() {
        var ownerPermissions = teamService.permissions(business.getId(), owner.getEmail());
        var adminPermissions = teamService.permissions(business.getId(), admin.getEmail());

        assertTrue(ownerPermissions.permissions().contains(
                MembershipPermission.MANAGE_ALL_MEMBERS
        ));
        assertFalse(adminPermissions.permissions().contains(
                MembershipPermission.MANAGE_ALL_MEMBERS
        ));
        assertTrue(adminPermissions.permissions().contains(
                MembershipPermission.MANAGE_STAFF
        ));
    }

    private Business business(String slug) {
        Business value = new Business();
        value.setName(slug);
        value.setSlug(slug);
        value.setCategoryCode("PROFESSIONAL_SERVICES");
        value.setActive(true);
        return value;
    }

    private User user(String email) {
        User value = new User();
        value.setFirstName("Team");
        value.setLastName("User");
        value.setEmail(email);
        value.setPassword("test-password-hash");
        value.setRole(Role.CLIENT);
        value.setActive(true);
        return value;
    }
}
