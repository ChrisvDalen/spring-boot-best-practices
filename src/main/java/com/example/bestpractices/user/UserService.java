package com.example.bestpractices.user;

import com.example.bestpractices.messaging.UserEvent;
import com.example.bestpractices.messaging.UserEventType;
import com.example.bestpractices.metrics.UserMetrics;
import com.example.bestpractices.outbox.OutboxEventService;
import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UpdateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Best practices demonstrated:
 * - Transactional Outbox: OutboxEventService.saveEvent() is called inside the same
 *   @Transactional method as the domain write. Both rows commit or roll back atomically.
 *   The OutboxEventProcessor then relays them to RabbitMQ asynchronously.
 * - MDC.get("correlationId") threads the HTTP request's correlation ID through to the
 *   event so consumers can join log lines from publisher and subscriber.
 * - UserMetrics.timeUserFind() wraps the cache-miss DB path; cache hits are so fast
 *   they do not need instrumentation.
 * - @Cacheable / @CachePut / @CacheEvict now target the Redis-backed RedisCacheManager,
 *   making the cache shared across all application instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String AGGREGATE_TYPE = "User";

    private final UserRepository userRepository;
    private final OutboxEventService outboxEventService;
    private final UserMetrics userMetrics;

    public Page<UserResponse> findAllActive(Pageable pageable) {
        return userRepository.findAllActive(pageable).map(this::toResponse);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse findById(Long id) {
        log.debug("Cache miss — fetching user {} from DB", id);
        return userMetrics.timeUserFind(() ->
                userRepository.findById(id)
                        .map(this::toResponse)
                        .orElseThrow(() -> new UserNotFoundException(id))
        );
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User saved = userRepository.save(user);
        UserResponse response = toResponse(saved);

        // Outbox write is inside this @Transactional — atomically paired with the user save
        outboxEventService.saveEvent(AGGREGATE_TYPE, saved.getId(), UserEventType.CREATED,
                UserEvent.of(UserEventType.CREATED, response, MDC.get("correlationId")));

        log.info("Created user id={} username={}", saved.getId(), saved.getUsername());
        return response;
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getActive() != null) user.setActive(request.getActive());

        UserResponse response = toResponse(userRepository.save(user));

        outboxEventService.saveEvent(AGGREGATE_TYPE, id, UserEventType.UPDATED,
                UserEvent.of(UserEventType.UPDATED, response, MDC.get("correlationId")));

        return response;
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);

        outboxEventService.saveEvent(AGGREGATE_TYPE, id, UserEventType.DELETED,
                UserEvent.deleted(id, MDC.get("correlationId")));

        log.info("Deleted user id={}", id);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
