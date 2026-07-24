package com.bookify.backend.user.repository;

import com.bookify.backend.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
