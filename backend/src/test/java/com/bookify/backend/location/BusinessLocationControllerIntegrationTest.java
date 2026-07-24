package com.bookify.backend.location;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.Role;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.bookify.backend.config.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessLocationControllerIntegrationTest {

    private static final String OWNER_EMAIL = "owner.locations@bookify.test";
    private static final String STAFF_EMAIL = "staff.locations@bookify.test";
    private static final String OUTSIDER_EMAIL = "outsider.locations@bookify.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private BusinessLocationRepository locationRepository;

    @Autowired
    private BusinessMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private Business firstBusiness;
    private Business secondBusiness;
    private BusinessLocation firstLocation;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();

        firstBusiness = businessRepository.save(business("location-business-one"));
        secondBusiness = businessRepository.save(business("location-business-two"));

        User owner = userRepository.save(user(OWNER_EMAIL));
        User staff = userRepository.save(user(STAFF_EMAIL));
        userRepository.save(user(OUTSIDER_EMAIL));

        membershipRepository.save(new BusinessMembership(firstBusiness, owner, MembershipRole.OWNER));
        membershipRepository.save(new BusinessMembership(secondBusiness, owner, MembershipRole.OWNER));
        membershipRepository.save(new BusinessMembership(firstBusiness, staff, MembershipRole.STAFF));

        firstLocation = locationRepository.save(location(firstBusiness, "Sede principal"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void ownerCanCreateListAndUpdateLocations() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/locations", firstBusiness.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("Sede norte", "America/Lima")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(firstBusiness.getId()))
                .andExpect(jsonPath("$.name").value("Sede norte"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/businesses/{businessId}/locations", firstBusiness.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/locations/{locationId}",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("Sede central", "America/Bogota")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sede central"))
                .andExpect(jsonPath("$.timezone").value("America/Bogota"));
    }

    @Test
    @WithMockUser(username = STAFF_EMAIL)
    void staffCanReadButCannotManageLocations() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/{businessId}/locations", firstBusiness.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/businesses/{businessId}/locations", firstBusiness.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("Sede denegada", "America/Lima")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OUTSIDER_EMAIL)
    void userWithoutMembershipCannotReadLocations() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/{businessId}/locations", firstBusiness.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void locationCannotBeReadThroughAnotherBusiness() throws Exception {
        BusinessLocation otherLocation = locationRepository.save(location(secondBusiness, "Otra sede"));

        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/locations/{locationId}",
                        firstBusiness.getId(),
                        otherLocation.getId()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void lastActiveLocationCannotBeDisabled() throws Exception {
        mockMvc.perform(patch(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/status",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A business must have at least one active location"));

        locationRepository.save(location(firstBusiness, "Sede secundaria"));

        mockMvc.perform(patch(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/status",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsDuplicateNameAndInvalidTimezone() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/locations", firstBusiness.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("sede PRINCIPAL", "America/Lima")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/businesses/{businessId}/locations", firstBusiness.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("Sede válida", "Lima/Invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Location timezone must be a valid IANA timezone"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void businessOwnerCannotSelfVerifyCoordinates() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/coordinates/verify",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"manual\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminVerifiesCoordinatesAndLocationEditInvalidatesThem() throws Exception {
        var principal = new AuthenticatedUser("platform.admin@bookify.test", "ADMIN");
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/coordinates/verify",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .with(authentication(authenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"verified-geocoder\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordinatesVerified").value(true))
                .andExpect(jsonPath("$.coordinateSource").value("verified-geocoder"))
                .andExpect(jsonPath("$.coordinatesVerifiedAt").isNotEmpty());

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/locations/{locationId}",
                        firstBusiness.getId(),
                        firstLocation.getId()
                )
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(OWNER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson("Sede actualizada", "America/Lima")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordinatesVerified").value(false))
                .andExpect(jsonPath("$.coordinateSource").doesNotExist());
    }

    private Business business(String slug) {
        Business business = new Business();
        business.setName(slug);
        business.setSlug(slug);
        business.setCategoryCode("PROFESSIONAL_SERVICES");
        business.setActive(true);
        return business;
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Location");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }

    private BusinessLocation location(Business business, String name) {
        return new BusinessLocation(
                business,
                name,
                "Av. Principal 123",
                "Lima",
                "PE",
                "America/Lima",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793")
        );
    }

    private String locationJson(String name, String timezone) {
        return """
                {
                  "name": "%s",
                  "address": "Av. Principal 456",
                  "city": "Lima",
                  "countryCode": "PE",
                  "timezone": "%s",
                  "latitude": -12.046374,
                  "longitude": -77.042793
                }
                """.formatted(name, timezone);
    }
}
