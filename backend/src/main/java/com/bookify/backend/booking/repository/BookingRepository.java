package com.bookify.backend.booking.repository;

import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findForUpdateByIdAndCustomerId(Long id, Long customerId);

    Optional<Booking> findByIdAndBusinessId(Long id, Long businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findForUpdateByIdAndBusinessId(Long id, Long businessId);

    List<Booking> findByCustomerIdOrderByStartsAtDesc(Long customerId);

    List<Booking> findByBusinessIdOrderByStartsAtDesc(Long businessId);

    @Query("""
            select booking
            from Booking booking
            where booking.resource.id in :resourceIds
              and booking.status in :statuses
              and booking.startsAt < :to
              and booking.endsAt > :from
            """)
    List<Booking> findActiveOverlapping(
            @Param("resourceIds") Collection<Long> resourceIds,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select (count(booking) > 0)
            from Booking booking
            where booking.resource.id = :resourceId
              and booking.status in :statuses
              and booking.startsAt < :endsAt
              and booking.endsAt > :startsAt
            """)
    boolean existsActiveOverlap(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt
    );
}
