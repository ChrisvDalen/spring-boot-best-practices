package com.example.bestpractices.integration;

import com.example.bestpractices.outbox.OutboxEventRepository;
import com.example.bestpractices.outbox.OutboxStatus;
import com.example.bestpractices.user.UserRepository;
import com.example.bestpractices.user.dto.CreateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Best practices demonstrated:
 * - Full-stack integration test: HTTP → Controller → Service → DB + Outbox → Processor → RabbitMQ
 * - Awaitility for async assertions — replaces fragile Thread.sleep() calls with a
 *   configurable poller that succeeds as soon as the condition is met
 * - TestRestTemplate (not MockMvc) exercises the real servlet container and filter chain,
 *   including the IdempotencyFilter
 * - Idempotency-Key test verifies the deduplication contract end-to-end against Redis
 * - Outbox polling test waits for OutboxEventProcessor to transition status to PUBLISHED,
 *   confirming the full async pipeline works
 */
class UserMessagingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_persistsOutboxEvent_andProcessorPublishesIt() throws Exception {
        var request = new CreateUserRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setFirstName("Alice");
        request.setLastName("Wonderland");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/api/v1/users", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Outbox event should be created immediately (same transaction)
        assertThat(outboxEventRepository.findAll())
                .anyMatch(e -> "CREATED".equals(e.getEventType().name())
                        && "alice".equals(userRepository.findById(e.getAggregateId())
                        .map(u -> u.getUsername()).orElse("")));

        // OutboxEventProcessor runs every 1 s — wait up to 10 s for PUBLISHED status
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(outboxEventRepository.findAll())
                                .anyMatch(e -> OutboxStatus.PUBLISHED.equals(e.getStatus()))
                );
    }

    @Test
    void createUser_withIdempotencyKey_deduplicatesSecondRequest() {
        var request = new CreateUserRequest();
        request.setUsername("bob");
        request.setEmail("bob@example.com");
        request.setFirstName("Bob");
        request.setLastName("Builder");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "create-bob-v1");
        var entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> first = restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/api/v1/users", entity, String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second call with the same Idempotency-Key must return 200 (cached replay, not 201)
        ResponseEntity<String> second = restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/api/v1/users", entity, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isEqualTo(first.getBody());

        // Only one user record should exist despite two requests
        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.findAll()).extracting("username")
                .filteredOn("bob"::equals).hasSize(1);
    }
}
