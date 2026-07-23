package com.bookify.backend.business.onboarding;

import com.bookify.backend.business.onboarding.dto.BusinessOnboardingResponse;
import com.bookify.backend.business.onboarding.dto.CreateBusinessRequest;
import com.bookify.backend.business.onboarding.dto.CreateLocationRequest;
import com.bookify.backend.business.onboarding.dto.MyBusinessResponse;
import com.bookify.backend.business.onboarding.service.BusinessOnboardingService;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.Role;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BusinessOnboardingServiceIntegrationTest {

    @Autowired
    private BusinessOnboardingService onboardingService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private BusinessLocationRepository locationRepository;

    @Autowired
    private BusinessMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(user("owner@bookify.test"));
    }

    @Test
    void createsBusinessLocationAndOwnerMembershipAtomically() {
        BusinessOnboardingResponse response = onboardingService.onboard(
                owner.getEmail(),
                request("studio-norte", "America/Lima")
        );

        assertEquals("studio-norte", response.slug());
        assertEquals(MembershipRole.OWNER, response.membershipRole());
        assertEquals("Lima", response.location().city());
        assertEquals(1, businessRepository.count());
        assertEquals(1, locationRepository.count());
        assertEquals(1, membershipRepository.count());

        List<MyBusinessResponse> businesses = onboardingService.findMyBusinesses(owner.getEmail());
        assertEquals(1, businesses.size());
        assertEquals(response.id(), businesses.get(0).id());
        assertEquals(MembershipRole.OWNER, businesses.get(0).membershipRole());
    }

    @Test
    void duplicateSlugDoesNotCreatePartialData() {
        onboardingService.onboard(owner.getEmail(), request("duplicate-slug", "America/Lima"));

        assertThrows(
                BadRequestException.class,
                () -> onboardingService.onboard(owner.getEmail(), request("duplicate-slug", "America/Bogota"))
        );

        assertEquals(1, businessRepository.count());
        assertEquals(1, locationRepository.count());
        assertEquals(1, membershipRepository.count());
    }

    @Test
    void invalidTimezoneDoesNotCreatePartialData() {
        assertThrows(
                BadRequestException.class,
                () -> onboardingService.onboard(owner.getEmail(), request("invalid-timezone", "Lima/Invalid"))
        );

        assertEquals(0, businessRepository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(0, membershipRepository.count());
    }

    private CreateBusinessRequest request(String slug, String timezone) {
        return new CreateBusinessRequest(
                "Studio Norte",
                slug,
                "BARBERSHOP",
                "Reservas para servicios profesionales",
                "+51 999 999 999",
                "contacto@studio.test",
                new CreateLocationRequest(
                        "Sede principal",
                        "Av. Principal 123",
                        "Lima",
                        "PE",
                        timezone,
                        new BigDecimal("-12.046374"),
                        new BigDecimal("-77.042793")
                )
        );
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Owner");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }
}
