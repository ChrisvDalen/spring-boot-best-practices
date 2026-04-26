package com.example.bestpractices.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Best practices demonstrated:
 * - Extend JpaRepository<T, ID> — gives you CRUD + pagination for free
 * - Prefer derived query methods for simple lookups; use @Query for anything complex
 * - Return Optional<T> from single-result finders to force callers to handle absence
 * - Accept Pageable in list queries to prevent unbounded result sets
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Explicit JPQL makes intent clear for non-trivial filtering
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findAllActive(Pageable pageable);
}
