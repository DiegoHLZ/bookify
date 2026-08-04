package com.bookify.backend.tenancy.repository;

import com.bookify.backend.tenancy.model.BusinessInvitation;
import com.bookify.backend.tenancy.model.InvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface BusinessInvitationRepository
        extends JpaRepository<BusinessInvitation, Long> {

    Optional<BusinessInvitation> findByBusinessIdAndEmailIgnoreCaseAndStatus(
            Long businessId,
            String email,
            InvitationStatus status
    );

    List<BusinessInvitation> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<BusinessInvitation> findByIdAndBusinessId(Long id, Long businessId);

    Optional<BusinessInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BusinessInvitation> findForUpdateByIdAndBusinessId(Long id, Long businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BusinessInvitation> findForUpdateByTokenHash(String tokenHash);
}
