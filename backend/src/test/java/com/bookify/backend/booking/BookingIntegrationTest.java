package com.bookify.backend.booking;

import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleRuleType;
import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.booking.repository.BookingStatusHistoryRepository;
import com.bookify.backend.booking.service.BookingService;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.OfferingLocation;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BookingConflictException;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-bookings;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class BookingIntegrationTest {

    private static final String CUSTOMER_EMAIL = "customer.booking@bookify.test";
    private static final String SECOND_CUSTOMER_EMAIL = "customer2.booking@bookify.test";
    private static final String OWNER_EMAIL = "owner.booking@bookify.test";
    private static final Instant START = Instant.parse("2026-07-27T14:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingStatusHistoryRepository historyRepository;
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

        business = businessRepository.save(business("booking-business"));
        location = locationRepository.save(location(business));
        service = serviceRepository.save(service(business));
        resource = resourceRepository.save(resource(business, location));
        offeringLocationRepository.save(new OfferingLocation(business, service, location));
        offeringResourceRepository.save(new OfferingResource(business, service, resource));
        ruleRepository.save(new ResourceScheduleRule(
                business,
                resource,
                DayOfWeek.MONDAY,
                ScheduleRuleType.AVAILABLE,
                LocalTime.parse("09:00"),
                LocalTime.parse("13:00")
        ));

        userRepository.save(user(CUSTOMER_EMAIL));
        userRepository.save(user(SECOND_CUSTOMER_EMAIL));
        User owner = userRepository.save(user(OWNER_EMAIL));
        membershipRepository.save(new BusinessMembership(
                business, owner, MembershipRole.OWNER
        ));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL)
    void createsAndReplaysBookingIdempotently() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "booking-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(START)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startsAt").value("2026-07-27T14:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2026-07-27T15:00:00Z"))
                .andExpect(jsonPath("$.localStart").value("2026-07-27T09:00:00"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "booking-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(START)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals(1, bookingRepository.count());
    }

    @Test
    @WithMockUser(username = SECOND_CUSTOMER_EMAIL)
    void rejectsOverlappingBookingAndRemovesOccupiedSlots() throws Exception {
        bookingService.create(
                CUSTOMER_EMAIL, "first-booking", request(START)
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "overlap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(START.plusSeconds(30 * 60))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "The requested booking is no longer available"
                ));

        mockMvc.perform(get(availabilityPath())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(5)))
                .andExpect(jsonPath("$.slots[0].localStart").value("2026-07-27T10:00:00"));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL)
    void cancellationIsIdempotentAndRestoresAvailability() throws Exception {
        Long bookingId = bookingService.create(
                CUSTOMER_EMAIL, "cancel-booking", request(START)
        ).id();

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get(availabilityPath())
                        .queryParam("from", "2026-07-27")
                        .queryParam("to", "2026-07-27")
                        .queryParam("intervalMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(4)));

        assertEquals(2, historyRepository.count());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL)
    void validatesIdempotencyAndRejectsNonGeneratedTimes() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(START)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Idempotency-Key header is required"
                ));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "outside-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(Instant.parse("2026-07-27T20:00:00Z"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL)
    void customerAndBusinessViewsAreAuthorized() throws Exception {
        bookingService.create(CUSTOMER_EMAIL, "view-booking", request(START));

        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/businesses/{id}/bookings", business.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void businessMemberCanListBookings() throws Exception {
        bookingService.create(CUSTOMER_EMAIL, "business-view", request(START));

        mockMvc.perform(get("/api/v1/businesses/{id}/bookings", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerEmail").value(CUSTOMER_EMAIL));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void businessMemberCompletesBookingAndReadsAuditHistory() throws Exception {
        Long bookingId = bookingService.create(
                CUSTOMER_EMAIL, "complete-booking", request(START)
        ).id();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(
                                "/api/v1/businesses/{businessId}/bookings/{bookingId}/status",
                                business.getId(),
                                bookingId
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED","reason":"Servicio realizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/bookings/{bookingId}/history",
                        business.getId(),
                        bookingId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$[0].toStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[1].fromStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[1].toStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[1].actorEmail").value(OWNER_EMAIL));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void rejectsInvalidTerminalTransitionsAndMissingReasons() throws Exception {
        Long bookingId = bookingService.create(
                CUSTOMER_EMAIL, "invalid-transition", request(START)
        ).id();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(
                                "/api/v1/businesses/{businessId}/bookings/{bookingId}/status",
                                business.getId(),
                                bookingId
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Reason is required when cancelling or rejecting a booking"
                ));

        bookingService.changeStatus(
                business.getId(),
                bookingId,
                com.bookify.backend.booking.model.BookingStatus.COMPLETED,
                "Completada",
                OWNER_EMAIL
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(
                                "/api/v1/businesses/{businessId}/bookings/{bookingId}/status",
                                business.getId(),
                                bookingId
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"NO_SHOW","reason":"Intento inválido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Booking cannot transition from COMPLETED to NO_SHOW"
                ));

        assertEquals(2, historyRepository.count());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL)
    void nonMemberCannotOperateOrReadBusinessHistory() throws Exception {
        Long bookingId = bookingService.create(
                CUSTOMER_EMAIL, "forbidden-operation", request(START)
        ).id();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(
                                "/api/v1/businesses/{businessId}/bookings/{bookingId}/status",
                                business.getId(),
                                bookingId
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                        "/api/v1/businesses/{businessId}/bookings/{bookingId}/history",
                        business.getId(),
                        bookingId
                ))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentRequestsCannotBookSameResource() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> first = executor.submit(() ->
                    concurrentCreate(CUSTOMER_EMAIL, "concurrent-1", ready, start));
            Future<Object> second = executor.submit(() ->
                    concurrentCreate(SECOND_CUSTOMER_EMAIL, "concurrent-2", ready, start));
            ready.await();
            start.countDown();

            List<Object> results = List.of(first.get(), second.get());
            long successes = results.stream()
                    .filter(result -> result instanceof Long)
                    .count();
            long conflicts = results.stream()
                    .filter(result -> result instanceof BookingConflictException)
                    .count();

            assertEquals(1, successes);
            assertEquals(1, conflicts);
            assertEquals(1, bookingRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private Object concurrentCreate(
            String email,
            String key,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            start.await();
            return bookingService.create(email, key, request(START)).id();
        } catch (Exception exception) {
            assertInstanceOf(BookingConflictException.class, exception);
            return exception;
        }
    }

    private CreateBookingRequest request(Instant startsAt) {
        return new CreateBookingRequest(
                business.getId(),
                location.getId(),
                service.getId(),
                resource.getId(),
                startsAt,
                null,
                1,
                "Primera visita"
        );
    }

    private String requestBody(Instant startsAt) {
        return """
                {
                  "businessId": %d,
                  "locationId": %d,
                  "serviceId": %d,
                  "resourceId": %d,
                  "startsAt": "%s",
                  "notes": "Primera visita"
                }
                """.formatted(
                business.getId(),
                location.getId(),
                service.getId(),
                resource.getId(),
                startsAt
        );
    }

    private String availabilityPath() {
        return "/api/v1/businesses/" + business.getId()
                + "/locations/" + location.getId()
                + "/services/" + service.getId() + "/availability";
    }

    private Business business(String slug) {
        Business value = new Business();
        value.setName(slug);
        value.setSlug(slug);
        value.setCategoryCode("PROFESSIONAL_SERVICES");
        value.setActive(true);
        return value;
    }

    private BusinessLocation location(Business owner) {
        return new BusinessLocation(
                owner,
                "Sede principal",
                "Av. Principal 123",
                "Lima",
                "PE",
                "America/Lima",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793")
        );
    }

    private ServiceOffering service(Business owner) {
        ServiceOffering value = new ServiceOffering();
        value.setBusiness(owner);
        value.setName("Consulta");
        value.setDescription("Consulta profesional");
        value.setDurationMinutes(60);
        value.setPrice(new BigDecimal("80.00"));
        value.setCurrency("PEN");
        value.setActive(true);
        return value;
    }

    private BookableResource resource(
            Business owner,
            BusinessLocation selectedLocation
    ) {
        return new BookableResource(
                owner,
                selectedLocation,
                "Profesional 1",
                null,
                ResourceType.PROFESSIONAL,
                1
        );
    }

    private User user(String email) {
        User value = new User();
        value.setFirstName("Booking");
        value.setLastName("Tester");
        value.setEmail(email);
        value.setPassword("test-password-hash");
        value.setRole(Role.CLIENT);
        value.setActive(true);
        return value;
    }
}
