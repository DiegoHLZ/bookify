package com.bookify.backend.availability;

import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.ResourceType;
import com.bookify.backend.resource.repository.BookableResourceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-schedules;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class ResourceScheduleIntegrationTest {

    private static final String OWNER_EMAIL = "owner.schedule@bookify.test";
    private static final String STAFF_EMAIL = "staff.schedule@bookify.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private ResourceScheduleRuleRepository ruleRepository;
    @Autowired private ResourceScheduleExceptionRepository exceptionRepository;
    @Autowired private BookableResourceRepository resourceRepository;
    @Autowired private BusinessMembershipRepository membershipRepository;
    @Autowired private BusinessLocationRepository locationRepository;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;

    private Business business;
    private BusinessLocation location;
    private BookableResource resource;
    private BookableResource otherResource;

    @BeforeEach
    void setUp() {
        exceptionRepository.deleteAll();
        ruleRepository.deleteAll();
        resourceRepository.deleteAll();
        membershipRepository.deleteAll();
        locationRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();

        business = businessRepository.save(business("schedule-business"));
        Business otherBusiness = businessRepository.save(business("other-schedule-business"));
        location = locationRepository.save(location(business, "Sede principal"));
        BusinessLocation otherLocation =
                locationRepository.save(location(otherBusiness, "Sede externa"));
        resource = resourceRepository.save(resource(business, location, "Profesional 1"));
        otherResource = resourceRepository.save(
                resource(otherBusiness, otherLocation, "Profesional externo")
        );

        User owner = userRepository.save(user(OWNER_EMAIL));
        User staff = userRepository.save(user(STAFF_EMAIL));
        membershipRepository.save(new BusinessMembership(business, owner, MembershipRole.OWNER));
        membershipRepository.save(new BusinessMembership(business, staff, MembershipRole.STAFF));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void replacesAndReadsWeeklyScheduleWithBreaks() throws Exception {
        mockMvc.perform(put(schedulePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSchedule()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Lima"))
                .andExpect(jsonPath("$.rules", hasSize(3)));

        mockMvc.perform(get(schedulePath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void overlappingReplacementIsRejectedAndPreviousScheduleRemains() throws Exception {
        mockMvc.perform(put(schedulePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSchedule()))
                .andExpect(status().isOk());

        mockMvc.perform(put(schedulePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rules":[
                                  {"dayOfWeek":"MONDAY","ruleType":"AVAILABLE",
                                   "startTime":"09:00","endTime":"14:00"},
                                  {"dayOfWeek":"MONDAY","ruleType":"AVAILABLE",
                                   "startTime":"13:00","endTime":"18:00"}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Available intervals cannot overlap"));

        assertEquals(3, ruleRepository.count());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void breakMustBeInsideAvailableInterval() throws Exception {
        mockMvc.perform(put(schedulePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rules":[
                                  {"dayOfWeek":"MONDAY","ruleType":"AVAILABLE",
                                   "startTime":"09:00","endTime":"17:00"},
                                  {"dayOfWeek":"MONDAY","ruleType":"BREAK",
                                   "startTime":"17:00","endTime":"18:00"}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Each break must be contained in an available interval"
                ));
    }

    @Test
    @WithMockUser(username = STAFF_EMAIL)
    void staffCanReadButCannotReplaceSchedule() throws Exception {
        mockMvc.perform(get(schedulePath())).andExpect(status().isOk());
        mockMvc.perform(put(schedulePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSchedule()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void crossTenantResourceCannotBeConfiguredThroughPath() throws Exception {
        mockMvc.perform(put(schedulePath(otherResource.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSchedule()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void upsertsClosedAndCustomHoursExceptionsAndListsByRange() throws Exception {
        String exceptionPath = exceptionPath("2026-12-25");
        mockMvc.perform(put(exceptionPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exceptionType":"CLOSED","reason":"Navidad"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exceptionType").value("CLOSED"));

        mockMvc.perform(put(exceptionPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exceptionType":"CUSTOM_HOURS",
                                  "startTime":"10:00",
                                  "endTime":"14:00",
                                  "reason":"Horario especial"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exceptionType").value("CUSTOM_HOURS"))
                .andExpect(jsonPath("$.startTime").value("10:00:00"));

        assertEquals(1, exceptionRepository.count());

        mockMvc.perform(get(exceptionsPath())
                        .queryParam("from", "2026-12-01")
                        .queryParam("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsInvalidExceptionShapesAndUnboundedRanges() throws Exception {
        mockMvc.perform(put(exceptionPath("2026-12-24"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exceptionType":"CLOSED",
                                  "startTime":"09:00",
                                  "endTime":"12:00"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(exceptionPath("2026-12-24"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exceptionType":"CUSTOM_HOURS","startTime":"15:00","endTime":"10:00"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(exceptionsPath())
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2028-01-01"))
                .andExpect(status().isBadRequest());
    }

    private String schedulePath() {
        return schedulePath(resource.getId());
    }

    private String schedulePath(Long resourceId) {
        return "/api/v1/businesses/" + business.getId()
                + "/locations/" + location.getId()
                + "/resources/" + resourceId + "/schedule";
    }

    private String exceptionPath(String date) {
        return exceptionsPath() + "/" + date;
    }

    private String exceptionsPath() {
        return "/api/v1/businesses/" + business.getId()
                + "/locations/" + location.getId()
                + "/resources/" + resource.getId() + "/exceptions";
    }

    private String validSchedule() {
        return """
                {"rules":[
                  {"dayOfWeek":"MONDAY","ruleType":"AVAILABLE",
                   "startTime":"09:00","endTime":"13:00"},
                  {"dayOfWeek":"MONDAY","ruleType":"BREAK",
                   "startTime":"11:00","endTime":"11:30"},
                  {"dayOfWeek":"MONDAY","ruleType":"AVAILABLE",
                   "startTime":"14:00","endTime":"18:00"}
                ]}
                """;
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
                owner, name, "Av. Principal 123", "Lima", "PE", "America/Lima",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793")
        );
    }

    private BookableResource resource(
            Business owner,
            BusinessLocation location,
            String name
    ) {
        return new BookableResource(
                owner, location, name, null, ResourceType.PROFESSIONAL, 1
        );
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Schedule");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPassword("test-password-hash");
        user.setRole(Role.CLIENT);
        user.setActive(true);
        return user;
    }
}
