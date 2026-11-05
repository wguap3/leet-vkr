package com.stackr.api.repository;

import com.stackr.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findForSubmission(@org.springframework.data.repository.query.Param("id") Long id);

    long countByRoleAndActiveTrue(com.stackr.api.entity.enums.Role role);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.rating DESC, u.id ASC")
    Page<User> findAllByOrderByRatingDesc(Pageable pageable);
}
