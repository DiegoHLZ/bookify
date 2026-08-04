package com.bookify.backend.capacity;

import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.booking.repository.BookingStatusHistoryRepository;
import com.bookify.backend.booking.service.BookingService;
import com.bookify.backend.business.model.*;
import com.bookify.backend.business.repository.*;
import com.bookify.backend.capacity.dto.CreateCapacitySessionRequest;
import com.bookify.backend.capacity.model.CapacitySession;
import com.bookify.backend.capacity.repository.CapacitySessionRepository;
import com.bookify.backend.capacity.service.CapacitySessionService;
import com.bookify.backend.common.exception.BookingConflictException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.*;
import com.bookify.backend.resource.repository.*;
import com.bookify.backend.tenancy.model.*;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.*;
import com.bookify.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookify-capacity;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class CapacitySessionIntegrationTest {
    private static final String OWNER = "owner.capacity@bookify.test";
    private static final String CUSTOMER_1 = "customer1.capacity@bookify.test";
    private static final String CUSTOMER_2 = "customer2.capacity@bookify.test";
    private static final Instant START = Instant.parse("2099-07-27T15:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired BookingService bookingService;
    @Autowired CapacitySessionService sessionService;
    @Autowired CapacitySessionRepository sessionRepository;
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

    private Business business;
    private BusinessLocation location;
    private ServiceOffering service;
    private BookableResource resource;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        bookingRepository.deleteAll();
        sessionRepository.deleteAll();
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
        location = locationRepository.save(location());
        service = serviceRepository.save(service());
        resource = resourceRepository.save(resource());
        offeringLocationRepository.save(new OfferingLocation(business, service, location));
        offeringResourceRepository.save(new OfferingResource(business, service, resource));
        userRepository.save(user(CUSTOMER_1));
        userRepository.save(user(CUSTOMER_2));
        User owner = userRepository.save(user(OWNER));
        membershipRepository.save(new BusinessMembership(
                business, owner, MembershipRole.OWNER
        ));
    }

    @Test
    @WithMockUser(username = CUSTOMER_1)
    void availabilityExposesConcreteSessionAndRemainingCapacity() throws Exception {
        CapacitySession session = createSession(5);

        mockMvc.perform(get(availabilityPath())
                        .queryParam("from", "2099-07-27")
                        .queryParam("to", "2099-07-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(1)))
                .andExpect(jsonPath("$.slots[0].capacitySessionId").value(session.getId()))
                .andExpect(jsonPath("$.slots[0].remainingCapacity").value(5))
                .andExpect(jsonPath("$.slots[0].startAt").value(START.toString()));
    }

    @Test
    @WithMockUser(username = CUSTOMER_1)
    void reservesQuantitiesAndHidesFullSession() throws Exception {
        CapacitySession session = createSession(5);
        bookingService.create(CUSTOMER_1, "capacity-1", request(session, 3));
        bookingService.create(CUSTOMER_2, "capacity-2", request(session, 2));

        CapacitySession updated = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(5, updated.getCapacityReserved());

        mockMvc.perform(get(availabilityPath())
                        .queryParam("from", "2099-07-27")
                        .queryParam("to", "2099-07-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(0)));
    }

    @Test
    void cancellationReleasesCapacityAndActiveBookingPreventsSessionCancellation() {
        CapacitySession session = createSession(4);
        Long bookingId = bookingService.create(
                CUSTOMER_1, "capacity-cancel", request(session, 3)
        ).id();

        assertThrows(
                com.bookify.backend.common.exception.BadRequestException.class,
                () -> sessionService.cancel(business.getId(), session.getId())
        );

        bookingService.cancel(bookingId, CUSTOMER_1);
        CapacitySession updated = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(0, updated.getCapacityReserved());
        assertEquals("CANCELLED", sessionService.cancel(
                business.getId(), session.getId()
        ).status().name());
    }

    @Test
    void concurrentReservationsCannotOversellSession() throws Exception {
        CapacitySession session = createSession(3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Future<Object> first = executor.submit(() ->
                    concurrentCreate(CUSTOMER_1, "race-capacity-1", session, ready, go));
            Future<Object> second = executor.submit(() ->
                    concurrentCreate(CUSTOMER_2, "race-capacity-2", session, ready, go));
            ready.await();
            go.countDown();
            List<Object> results = List.of(first.get(), second.get());

            assertEquals(1, results.stream().filter(Long.class::isInstance).count());
            assertEquals(1, results.stream()
                    .filter(BookingConflictException.class::isInstance).count());
            assertEquals(2, sessionRepository.findById(session.getId())
                    .orElseThrow().getCapacityReserved());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exclusiveServiceRejectsCapacityShape() {
        service.setBookingMode(BookingMode.EXCLUSIVE_RESOURCE);
        serviceRepository.saveAndFlush(service);
        CapacitySession session = createRawSession(5);
        assertThrows(
                com.bookify.backend.common.exception.BadRequestException.class,
                () -> bookingService.create(
                        CUSTOMER_1, "wrong-mode", request(session, 2)
                )
        );
    }

    private Object concurrentCreate(
            String email, String key, CapacitySession session,
            CountDownLatch ready, CountDownLatch go
    ) {
        ready.countDown();
        try {
            go.await();
            return bookingService.create(email, key, request(session, 2)).id();
        } catch (Exception exception) {
            assertInstanceOf(BookingConflictException.class, exception);
            return exception;
        }
    }

    private CapacitySession createSession(int capacity) {
        Long id = sessionService.create(
                business.getId(), location.getId(), service.getId(),
                new CreateCapacitySessionRequest(resource.getId(), START, capacity)
        ).id();
        return sessionRepository.findById(id).orElseThrow();
    }

    private CapacitySession createRawSession(int capacity) {
        return sessionRepository.saveAndFlush(new CapacitySession(
                business, location, service, resource, START,
                START.plusSeconds(60 * 60), capacity
        ));
    }

    private CreateBookingRequest request(CapacitySession session, int quantity) {
        return new CreateBookingRequest(
                business.getId(), location.getId(), service.getId(), resource.getId(),
                START, session.getId(), quantity, "Reserva grupal"
        );
    }

    private String availabilityPath() {
        return "/api/v1/businesses/" + business.getId()
                + "/locations/" + location.getId()
                + "/services/" + service.getId() + "/availability";
    }

    private Business business() {
        Business value = new Business();
        value.setName("Capacity business");
        value.setSlug("capacity-business");
        value.setCategoryCode("PROFESSIONAL_SERVICES");
        value.setActive(true);
        return value;
    }

    private BusinessLocation location() {
        return new BusinessLocation(
                business, "Sede", "Av. Test 1", "Lima", "PE", "America/Lima",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793")
        );
    }

    private ServiceOffering service() {
        ServiceOffering value = new ServiceOffering();
        value.setBusiness(business);
        value.setName("Clase grupal");
        value.setDurationMinutes(60);
        value.setPrice(new BigDecimal("25.00"));
        value.setCurrency("PEN");
        value.setActive(true);
        value.setBookingMode(BookingMode.CAPACITY_SESSION);
        return value;
    }

    private BookableResource resource() {
        return new BookableResource(
                business, location, "Sala grupal", null, ResourceType.ROOM, 20
        );
    }

    private User user(String email) {
        User value = new User();
        value.setFirstName("Capacity");
        value.setLastName("Tester");
        value.setEmail(email);
        value.setPassword("hash");
        value.setRole(Role.CLIENT);
        value.setActive(true);
        return value;
    }
}
