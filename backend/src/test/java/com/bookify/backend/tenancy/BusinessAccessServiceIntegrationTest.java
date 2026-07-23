package com.bookify.backend.tenancy;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import com.bookify.backend.user.model.Role;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BusinessAccessServiceIntegrationTest {

    @Autowired
    private BusinessAccessService businessAccessService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessMembershipRepository membershipRepository;

    private Business firstBusiness;
    private Business secondBusiness;
    private User owner;
    private User staff;

    @BeforeEach
    void setUp() {
        firstBusiness = businessRepository.save(business("first-business"));
        secondBusiness = businessRepository.save(business("second-business"));
        owner = userRepository.save(user("owner@bookify.test"));
        staff = userRepository.save(user("staff@bookify.test"));

        membershipRepository.save(
                new BusinessMembership(firstBusiness, owner, MembershipRole.OWNER)
        );
        membershipRepository.save(
                new BusinessMembership(firstBusiness, staff, MembershipRole.STAFF)
        );
    }

    @Test
    void activeMembershipAllowsOnlyItsOwnBusiness() {
        assertDoesNotThrow(() ->
                businessAccessService.requireMembership(firstBusiness.getId(), owner.getEmail())
        );

        assertThrows(AccessDeniedException.class, () ->
                businessAccessService.requireMembership(secondBusiness.getId(), owner.getEmail())
        );
    }

    @Test
    void staffCannotPerformManagementOperations() {
        assertDoesNotThrow(() ->
                businessAccessService.requireManagementAccess(firstBusiness.getId(), owner.getEmail())
        );

        assertThrows(AccessDeniedException.class, () ->
                businessAccessService.requireManagementAccess(firstBusiness.getId(), staff.getEmail())
        );
    }

    private Business business(String slug) {
        Business business = new Business();
        business.setName(slug);
        business.setSlug(slug);
        business.setActive(true);
        return business;
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }
}
