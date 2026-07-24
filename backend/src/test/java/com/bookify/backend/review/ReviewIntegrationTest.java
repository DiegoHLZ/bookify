package com.bookify.backend.review;

import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleRuleType;
import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.booking.repository.BookingStatusHistoryRepository;
import com.bookify.backend.booking.service.BookingService;
import com.bookify.backend.business.model.*;
import com.bookify.backend.business.repository.*;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.*;
import com.bookify.backend.resource.repository.*;
import com.bookify.backend.review.repository.ReviewRepository;
import com.bookify.backend.tenancy.model.*;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.*;
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
import java.time.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-reviews;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class ReviewIntegrationTest {
    private static final String CUSTOMER = "review.customer@bookify.test";
    private static final String SECOND = "review.second@bookify.test";
    private static final String OWNER = "review.owner@bookify.test";
    private static final Instant START = Instant.parse("2026-07-27T14:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired BookingService bookingService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired BookingStatusHistoryRepository historyRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired ResourceScheduleExceptionRepository exceptionRepository;
    @Autowired ResourceScheduleRuleRepository ruleRepository;
    @Autowired OfferingResourceRepository offeringResourceRepository;
    @Autowired OfferingLocationRepository offeringLocationRepository;
    @Autowired BookableResourceRepository resourceRepository;
    @Autowired ServiceOfferingRepository serviceRepository;
    @Autowired BusinessMembershipRepository membershipRepository;
    @Autowired BusinessLocationRepository locationRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired UserRepository userRepository;

    Business business;
    BusinessLocation location;
    ServiceOffering service;
    BookableResource resource;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        historyRepository.deleteAll();
        bookingRepository.deleteAll();
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

        business = businessRepository.save(business());
        location = locationRepository.save(new BusinessLocation(
                business, "Principal", "Av. Uno", "Lima", "PE", "America/Lima",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793")
        ));
        service = new ServiceOffering();
        service.setBusiness(business);
        service.setName("Consulta");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("50.00"));
        service.setCurrency("PEN");
        service.setActive(true);
        service = serviceRepository.save(service);
        resource = resourceRepository.save(new BookableResource(
                business, location, "Profesional", null, ResourceType.PROFESSIONAL, 1
        ));
        offeringLocationRepository.save(new OfferingLocation(business, service, location));
        offeringResourceRepository.save(new OfferingResource(business, service, resource));
        ruleRepository.save(new ResourceScheduleRule(
                business, resource, DayOfWeek.MONDAY, ScheduleRuleType.AVAILABLE,
                LocalTime.of(9, 0), LocalTime.of(13, 0)
        ));
        userRepository.save(user(CUSTOMER));
        userRepository.save(user(SECOND));
        User owner = userRepository.save(user(OWNER));
        membershipRepository.save(new BusinessMembership(
                business, owner, MembershipRole.OWNER
        ));
    }

    @Test
    @WithMockUser(username = CUSTOMER)
    void createsVerifiedReviewForCompletedBooking() throws Exception {
        Long bookingId = completedBooking(CUSTOMER, "review-1", START);

        mockMvc.perform(post("/api/v1/bookings/{id}/review", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"comment\":\" Excelente atención \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente atención"));

        mockMvc.perform(get("/api/v1/businesses/{id}/rating", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @WithMockUser(username = CUSTOMER)
    void rejectsReviewBeforeCompletionAndDuplicateReview() throws Exception {
        Long active = createBooking(CUSTOMER, "active", START);
        mockMvc.perform(post("/api/v1/bookings/{id}/review", active)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only completed bookings can be reviewed"
                ));

        Long completed = completedBooking(CUSTOMER, "completed", START.plusSeconds(3600));
        mockMvc.perform(post("/api/v1/bookings/{id}/review", completed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/bookings/{id}/review", completed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking already has a review"));
    }

    @Test
    @WithMockUser(username = SECOND)
    void customerCannotReviewAnotherCustomersBooking() throws Exception {
        Long bookingId = completedBooking(CUSTOMER, "owned", START);
        mockMvc.perform(post("/api/v1/bookings/{id}/review", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = CUSTOMER)
    void calculatesStableAggregateAndListsVerifiedReviews() throws Exception {
        Long first = completedBooking(CUSTOMER, "first-rating", START);
        Long second = completedBooking(SECOND, "second-rating", START.plusSeconds(3600));
        review(first, 4);

        mockMvc.perform(post("/api/v1/bookings/{id}/review", second)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(SECOND))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/businesses/{id}/rating", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(4.5))
                .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(get("/api/v1/businesses/{id}/reviews", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        assertEquals(2, reviewRepository.count());
    }

    private void review(Long bookingId, int score) throws Exception {
        mockMvc.perform(post("/api/v1/bookings/{id}/review", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":" + score + "}"))
                .andExpect(status().isCreated());
    }

    private Long completedBooking(String email, String key, Instant start) {
        Long id = createBooking(email, key, start);
        bookingService.changeStatus(
                business.getId(), id, BookingStatus.COMPLETED, "Servicio realizado", OWNER
        );
        return id;
    }

    private Long createBooking(String email, String key, Instant start) {
        return bookingService.create(email, key, new CreateBookingRequest(
                business.getId(), location.getId(), service.getId(), resource.getId(),
                start, null, 1, null
        )).id();
    }

    private Business business() {
        Business value = new Business();
        value.setName("Review business");
        value.setSlug("review-business");
        value.setCategoryCode("PROFESSIONAL_SERVICES");
        value.setActive(true);
        return value;
    }

    private User user(String email) {
        User value = new User();
        value.setFirstName("Review");
        value.setLastName("Tester");
        value.setEmail(email);
        value.setPassword("hash");
        value.setRole(Role.CLIENT);
        value.setActive(true);
        return value;
    }
}
