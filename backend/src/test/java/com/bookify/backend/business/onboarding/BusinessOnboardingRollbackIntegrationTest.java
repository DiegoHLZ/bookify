package com.bookify.backend.business.onboarding;

import com.bookify.backend.business.onboarding.dto.CreateBusinessRequest;
import com.bookify.backend.business.onboarding.dto.CreateLocationRequest;
import com.bookify.backend.business.onboarding.service.BusinessOnboardingService;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.Role;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class BusinessOnboardingRollbackIntegrationTest {

    @Autowired
    private BusinessOnboardingService onboardingService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private BusinessLocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private BusinessMembershipRepository membershipRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(user());
    }

    @Test
    void rollsBackBusinessAndLocationWhenMembershipCreationFails() {
        doThrow(new IllegalStateException("simulated membership failure"))
                .when(membershipRepository)
                .save(any(BusinessMembership.class));

        assertThrows(
                IllegalStateException.class,
                () -> onboardingService.onboard(owner.getEmail(), request())
        );

        assertEquals(0, businessRepository.count());
        assertEquals(0, locationRepository.count());
    }

    private CreateBusinessRequest request() {
        return new CreateBusinessRequest(
                "Rollback Studio",
                "rollback-studio",
                "BARBERSHOP",
                null,
                null,
                null,
                new CreateLocationRequest(
                        "Sede principal",
                        "Av. Principal 123",
                        "Lima",
                        "PE",
                        "America/Lima",
                        new BigDecimal("-12.046374"),
                        new BigDecimal("-77.042793")
                )
        );
    }

    private User user() {
        User user = new User();
        user.setFirstName("Rollback");
        user.setLastName("Owner");
        user.setEmail("rollback@bookify.test");
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }
}
