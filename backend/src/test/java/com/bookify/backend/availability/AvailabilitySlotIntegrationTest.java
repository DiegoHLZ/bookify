package com.bookify.backend.availability;

import com.bookify.backend.availability.model.ResourceScheduleException;
import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleExceptionType;
import com.bookify.backend.availability.model.ScheduleRuleType;
import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.OfferingLocation;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.OfferingResource;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-availability;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class AvailabilitySlotIntegrationTest {

    private static final String OWNER_EMAIL = "owner.availability@bookify.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private ResourceScheduleExceptionRepository exceptionRepository;
    @Autowired private ResourceScheduleRuleRepository ruleRepository;
    @Autowired private OfferingResourceRepository offeringResourceRepository;
    @Autowired private OfferingLocationRepository offeringLocationRepository;
    @Autowired private BookableResourceRepository resourceRepository;
    @Autowired private ServiceOfferingRepository serviceRepository;
    @Autowired private BusinessMembershipRepository membershipRepository;
    @Autowired private BusinessLocationRepository locationRepository;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;

    private Business business;
    private BusinessLocation location;
    private ServiceOffering service;
    private BookableResource resource;

    @BeforeEach
    void setUp() {
        exceptionRepository.deleteAll();
        ruleRepository.deleteAll();
        offeringResourceRepository.deleteAll();
        offeringLocationRepository.deleteAll();
        resourceRepository.deleteAll();
        serviceRepository.deleteAll();
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();

        business = businessRepository.save(business("availability-business"));
        location = locationRepository.save(location(business, "America/Lima"));
        service = serviceRepository.save(service(business, 60));
        resource = resourceRepository.save(resource(business, location, "Profesional 1"));
        offeringLocationRepository.save(new OfferingLocation(business, service, location));
        offeringResourceRepository.save(new OfferingResource(business, service, resource));

        User owner = userRepository.save(user(OWNER_EMAIL));
        membershipRepository.save(new BusinessMembership(business, owner, MembershipRole.OWNER));
    }

    @Test
    @WithMockUser(username = "customer@bookify.test")
    void generatesConcreteUtcSlotsAroundBreaks() throws Exception {
        saveRule(DayOfWeek.MONDAY, ScheduleRuleType.AVAILABLE, "09:00", "13:00");
        saveRule(DayOfWeek.MONDAY, ScheduleRuleType.BREAK, "11:00", "11:30");

        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.timezone").value("America/Lima"))
                .andExpect(jsonPath("$.slots", hasSize(5)))
                .andExpect(jsonPath("$.slots[0].localStart").value("2026-07-27T09:00:00"))
                .andExpect(jsonPath("$.slots[0].startAt").value("2026-07-27T14:00:00Z"))
                .andExpect(jsonPath("$.slots[2].localStart").value("2026-07-27T10:00:00"))
                .andExpect(jsonPath("$.slots[3].localStart").value("2026-07-27T11:30:00"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void closedAndCustomHoursExceptionsTakePrecedence() throws Exception {
        saveRule(DayOfWeek.MONDAY, ScheduleRuleType.AVAILABLE, "09:00", "17:00");
        saveRule(DayOfWeek.MONDAY, ScheduleRuleType.BREAK, "11:00", "11:30");
        saveException(
                LocalDate.parse("2026-07-27"),
                ScheduleExceptionType.CUSTOM_HOURS,
                "10:00",
                "12:30"
        );
        saveException(
                LocalDate.parse("2026-07-28"),
                ScheduleExceptionType.CLOSED,
                null,
                null
        );

        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-28")
                        .queryParam("intervalMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(2)))
                .andExpect(jsonPath("$.slots[0].localStart").value("2026-07-27T10:00:00"))
                .andExpect(jsonPath("$.slots[1].localStart").value("2026-07-27T11:30:00"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void combinesSlotsFromEveryActiveAssignedResource() throws Exception {
        BookableResource second =
                resourceRepository.save(resource(business, location, "Profesional 2"));
        offeringResourceRepository.save(new OfferingResource(business, service, second));
        saveRule(resource, DayOfWeek.MONDAY, ScheduleRuleType.AVAILABLE, "09:00", "10:00");
        saveRule(second, DayOfWeek.MONDAY, ScheduleRuleType.AVAILABLE, "09:00", "10:00");

        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(2)))
                .andExpect(jsonPath("$.slots[0].resourceId").value(resource.getId()))
                .andExpect(jsonPath("$.slots[1].resourceId").value(second.getId()));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void handlesDstGapsAndOverlapsAsConcreteInstants() throws Exception {
        BusinessLocation newYork =
                locationRepository.save(location(business, "America/New_York"));
        offeringLocationRepository.save(new OfferingLocation(business, service, newYork));
        BookableResource dstResource =
                resourceRepository.save(resource(business, newYork, "DST resource"));
        offeringResourceRepository.save(new OfferingResource(business, service, dstResource));
        saveRule(
                dstResource, DayOfWeek.SUNDAY, ScheduleRuleType.AVAILABLE, "01:00", "04:00"
        );

        mockMvc.perform(get(path(newYork))
                        .queryParam("from", "2026-03-08")
                        .queryParam("to", "2026-03-08")
                        .queryParam("intervalMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(3)));

        mockMvc.perform(get(path(newYork))
                        .queryParam("from", "2026-11-01")
                        .queryParam("to", "2026-11-01")
                        .queryParam("intervalMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(7)))
                .andExpect(jsonPath("$.slots[0].localStart").value("2026-11-01T01:00:00"))
                .andExpect(jsonPath("$.slots[2].localStart").value("2026-11-01T01:00:00"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsInvalidRangesAndIntervals() throws Exception {
        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Availability range cannot exceed 31 days"
                ));

        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(path())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "1440"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsServiceNotOfferedAtRequestedLocation() throws Exception {
        BusinessLocation otherLocation =
                locationRepository.save(location(business, "America/Bogota"));

        mockMvc.perform(get(path(otherLocation))
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27"))
                .andExpect(status().isNotFound());
    }

    private String path() {
        return path(location);
    }

    private String path(BusinessLocation selectedLocation) {
        return "/api/v1/businesses/" + business.getId()
                + "/locations/" + selectedLocation.getId()
                + "/services/" + service.getId() + "/availability";
    }

    private void saveRule(
            DayOfWeek day,
            ScheduleRuleType type,
            String start,
            String end
    ) {
        saveRule(resource, day, type, start, end);
    }

    private void saveRule(
            BookableResource selectedResource,
            DayOfWeek day,
            ScheduleRuleType type,
            String start,
            String end
    ) {
        ruleRepository.save(new ResourceScheduleRule(
                business,
                selectedResource,
                day,
                type,
                LocalTime.parse(start),
                LocalTime.parse(end)
        ));
    }

    private void saveException(
            LocalDate date,
            ScheduleExceptionType type,
            String start,
            String end
    ) {
        ResourceScheduleException exception =
                new ResourceScheduleException(business, resource, date);
        exception.update(
                type,
                start == null ? null : LocalTime.parse(start),
                end == null ? null : LocalTime.parse(end),
                null
        );
        exceptionRepository.save(exception);
    }

    private Business business(String slug) {
        Business value = new Business();
        value.setName(slug);
        value.setSlug(slug);
        value.setCategoryCode("PROFESSIONAL_SERVICES");
        value.setActive(true);
        return value;
    }

    private BusinessLocation location(Business owner, String timezone) {
        return new BusinessLocation(
                owner,
                "Location " + timezone + System.nanoTime(),
                "Av. Principal 123",
                "Lima",
                "PE",
                timezone,
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793")
        );
    }

    private ServiceOffering service(Business owner, int durationMinutes) {
        ServiceOffering value = new ServiceOffering();
        value.setBusiness(owner);
        value.setName("Consulta");
        value.setDescription("Consulta profesional");
        value.setDurationMinutes(durationMinutes);
        value.setPrice(new BigDecimal("80.00"));
        value.setCurrency("PEN");
        value.setActive(true);
        return value;
    }

    private BookableResource resource(
            Business owner,
            BusinessLocation selectedLocation,
            String name
    ) {
        return new BookableResource(
                owner, selectedLocation, name, null, ResourceType.PROFESSIONAL, 1
        );
    }

    private User user(String email) {
        User value = new User();
        value.setFirstName("Availability");
        value.setLastName("Tester");
        value.setEmail(email);
        value.setPassword("test-password-hash");
        value.setRole(Role.CLIENT);
        value.setActive(true);
        return value;
    }
}
