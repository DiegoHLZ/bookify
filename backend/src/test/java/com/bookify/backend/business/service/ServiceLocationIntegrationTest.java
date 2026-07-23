package com.bookify.backend.business.service;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-service-locations;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class ServiceLocationIntegrationTest {

    private static final String OWNER_EMAIL = "owner.services@bookify.test";
    private static final String STAFF_EMAIL = "staff.services@bookify.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OfferingLocationRepository offeringLocationRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private BusinessLocationRepository locationRepository;

    @Autowired
    private BusinessMembershipRepository membershipRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    private Business business;
    private Business otherBusiness;
    private BusinessLocation firstLocation;
    private BusinessLocation secondLocation;
    private BusinessLocation otherLocation;

    @BeforeEach
    void setUp() {
        offeringLocationRepository.deleteAll();
        serviceRepository.deleteAll();
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();

        business = businessRepository.save(business("service-business"));
        otherBusiness = businessRepository.save(business("other-service-business"));
        firstLocation = locationRepository.save(location(business, "Sede principal"));
        secondLocation = locationRepository.save(location(business, "Sede norte"));
        otherLocation = locationRepository.save(location(otherBusiness, "Sede externa"));

        User owner = userRepository.save(user(OWNER_EMAIL));
        User staff = userRepository.save(user(STAFF_EMAIL));
        membershipRepository.save(new BusinessMembership(business, owner, MembershipRole.OWNER));
        membershipRepository.save(new BusinessMembership(business, staff, MembershipRole.STAFF));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void createsServiceWithExactPriceAndMultipleLocations() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("39.90", firstLocation.getId(), secondLocation.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(39.90))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.locationIds", containsInAnyOrder(
                        firstLocation.getId().intValue(),
                        secondLocation.getId().intValue()
                )));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void crossTenantLocationRollsBackServiceCreation() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("45.00", firstLocation.getId(), otherLocation.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("All service locations must be active and belong to the business"));

        org.junit.jupiter.api.Assertions.assertEquals(0, serviceRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(0, offeringLocationRepository.count());
    }

    @Test
    @WithMockUser(username = STAFF_EMAIL)
    void staffCannotCreateServices() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("39.90", firstLocation.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void updateReplacesLocationAssignmentsAtomically() throws Exception {
        String response = mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services", business.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson("39.90", firstLocation.getId()))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long serviceId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .get("id")
                .asLong();

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/services/{serviceId}",
                        business.getId(),
                        serviceId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("49.95", secondLocation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(49.95))
                .andExpect(jsonPath("$.locationIds[0]").value(secondLocation.getId()))
                .andExpect(jsonPath("$.locationIds.length()").value(1));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsPriceWithMoreThanTwoDecimals() throws Exception {
        mockMvc.perform(post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("39.999", firstLocation.getId())))
                .andExpect(status().isBadRequest());
    }

    private Business business(String slug) {
        Business business = new Business();
        business.setName(slug);
        business.setSlug(slug);
        business.setCategoryCode("PROFESSIONAL_SERVICES");
        business.setActive(true);
        return business;
    }

    private BusinessLocation location(Business owner, String name) {
        return new BusinessLocation(
                owner,
                name,
                "Av. Principal 123",
                "Lima",
                "PE",
                "America/Lima",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793")
        );
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Service");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }

    private String createJson(String price, Long... locationIds) {
        return serviceJson(price, true, locationIds);
    }

    private String updateJson(String price, Long... locationIds) {
        return serviceJson(price, true, locationIds);
    }

    private String serviceJson(String price, boolean active, Long... locationIds) {
        String ids = java.util.Arrays.stream(locationIds)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "name": "Corte clásico",
                  "description": "Servicio de prueba",
                  "durationMinutes": 45,
                  "price": %s,
                  "currency": "PEN",
                  "active": %s,
                  "locationIds": [%s]
                }
                """.formatted(price, active, ids);
    }
}
