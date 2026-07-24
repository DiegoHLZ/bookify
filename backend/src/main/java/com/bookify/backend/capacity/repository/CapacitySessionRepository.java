package com.bookify.backend.capacity.repository;

import com.bookify.backend.capacity.model.CapacitySession;
import com.bookify.backend.capacity.model.CapacitySessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CapacitySessionRepository extends JpaRepository<CapacitySession, Long> {
    List<CapacitySession>
    findByBusinessIdAndLocationIdAndServiceIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Long businessId, Long locationId, Long serviceId,
            CapacitySessionStatus status, Instant from, Instant to
    );

    Optional<CapacitySession> findByIdAndBusinessId(Long id, Long businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CapacitySession> findForUpdateByIdAndBusinessId(Long id, Long businessId);
}
