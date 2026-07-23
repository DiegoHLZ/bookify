package com.bookify.backend.resource;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.OfferingLocation;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.ResourceType;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import com.bookify.backend.resource.repository.OfferingResourceRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-resources;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class BookableResourceIntegrationTest {

    private static final String OWNER_EMAIL = "owner.resources@bookify.test";
    private static final String STAFF_EMAIL = "staff.resources@bookify.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private OfferingResourceRepository offeringResourceRepository;
    @Autowired private BookableResourceRepository resourceRepository;
    @Autowired private OfferingLocationRepository offeringLocationRepository;
    @Autowired private ServiceOfferingRepository serviceRepository;
    @Autowired private BusinessMembershipRepository membershipRepository;
    @Autowired private BusinessLocationRepository locationRepository;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;

    private Business business;
    private Business otherBusiness;
    private BusinessLocation firstLocation;
    private BusinessLocation secondLocation;
    private BusinessLocation otherLocation;
    private ServiceOffering service;
    private BookableResource firstResource;
    private BookableResource secondResource;
    private BookableResource otherResource;

    @BeforeEach
    void setUp() {
        offeringResourceRepository.deleteAll();
        resourceRepository.deleteAll();
        offeringLocationRepository.deleteAll();
        serviceRepository.deleteAll();
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();

        business = businessRepository.save(business("resource-business"));
        otherBusiness = businessRepository.save(business("other-resource-business"));
        firstLocation = locationRepository.save(location(business, "Sede principal"));
        secondLocation = locationRepository.save(location(business, "Sede norte"));
        otherLocation = locationRepository.save(location(otherBusiness, "Sede externa"));

        service = serviceRepository.save(service(business));
        offeringLocationRepository.save(new OfferingLocation(business, service, firstLocation));

        firstResource = resourceRepository.save(resource(
                business, firstLocation, "Cancha 1", ResourceType.COURT, 1
        ));
        secondResource = resourceRepository.save(resource(
                business, secondLocation, "Sala norte", ResourceType.ROOM, 8
        ));
        otherResource = resourceRepository.save(resource(
                otherBusiness, otherLocation, "Equipo externo", ResourceType.EQUIPMENT, 1
        ));

        User owner = userRepository.save(user(OWNER_EMAIL));
        User staff = userRepository.save(user(STAFF_EMAIL));
        membershipRepository.save(new BusinessMembership(business, owner, MembershipRole.OWNER));
        membershipRepository.save(new BusinessMembership(business, staff, MembershipRole.STAFF));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void ownerCanCreateUpdateListAndDisableResources() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Escritorio 1", "DESK", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DESK"))
                .andExpect(jsonPath("$.capacity").value(1));

        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources/{resourceId}",
                        business.getId(), firstLocation.getId(), firstResource.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Cancha central", "COURT", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cancha central"))
                .andExpect(jsonPath("$.capacity").value(2));

        mockMvc.perform(patch(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources/{resourceId}/status",
                        business.getId(), firstLocation.getId(), firstResource.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = STAFF_EMAIL)
    void staffCanReadButCannotManageResources() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                ))
                .andExpect(status().isOk());

        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Denegado", "ROOM", 2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsInvalidCapacityDuplicateNameAndCrossTenantPath() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("Sin capacidad", "ROOM", 0)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources",
                        business.getId(), firstLocation.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson("cancha 1", "COURT", 1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/locations/{locationId}/resources/{resourceId}",
                        business.getId(), firstLocation.getId(), otherResource.getId()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void assignsResourcesOnlyWhereServiceIsOfferedAndPreservesPreviousAssignmentOnFailure()
            throws Exception {
        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/services/{serviceId}/resources",
                        business.getId(), service.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceIds\":[" + firstResource.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceIds[0]").value(firstResource.getId()));

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/services/{serviceId}/resources",
                        business.getId(), service.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceIds\":[" + secondResource.getId() + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Each resource must belong to a location where the service is offered"
                ));

        assertEquals(
                java.util.List.of(firstResource.getId()),
                offeringResourceRepository.findResourceIdsByServiceId(service.getId())
        );
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsCrossTenantAndInactiveResourceAssignments() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/services/{serviceId}/resources",
                        business.getId(), service.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceIds\":[" + otherResource.getId() + "]}"))
                .andExpect(status().isBadRequest());

        firstResource.setActive(false);
        resourceRepository.saveAndFlush(firstResource);

        mockMvc.perform(put(
                        "/api/v1/businesses/{businessId}/services/{serviceId}/resources",
                        business.getId(), service.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceIds\":[" + firstResource.getId() + "]}"))
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

    private BusinessLocation location(Business business, String name) {
        return new BusinessLocation(
                business, name, "Av. Principal 123", "Lima", "PE", "America/Lima",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793")
        );
    }

    private ServiceOffering service(Business business) {
        ServiceOffering service = new ServiceOffering();
        service.setBusiness(business);
        service.setName("Reserva de recurso");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("50.00"));
        service.setCurrency("PEN");
        service.setActive(true);
        return service;
    }

    private BookableResource resource(
            Business business,
            BusinessLocation location,
            String name,
            ResourceType type,
            int capacity
    ) {
        return new BookableResource(business, location, name, null, type, capacity);
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Resource");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }

    private String resourceJson(String name, String type, int capacity) {
        return """
                {
                  "name": "%s",
                  "description": "Recurso de prueba",
                  "type": "%s",
                  "capacity": %d
                }
                """.formatted(name, type, capacity);
    }
}
