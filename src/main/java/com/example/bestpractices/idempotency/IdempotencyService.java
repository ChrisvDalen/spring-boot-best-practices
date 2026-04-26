package com.example.bestpractices.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Best practices demonstrated:
 * - Redis SET NX (setIfAbsent) is atomic: exactly one concurrent request will win
 *   the "PROCESSING" slot; others see a non-null value and can short-circuit
 * - 24-hour TTL prevents Redis from growing unbounded — keys auto-expire after
 *   the window during which clients might reasonably retry
 * - Separate PROCESSING sentinel distinguishes "in-flight" from "completed":
 *   callers can differentiate 409 Conflict from a 200 cache hit
 * - Prefixing keys with "idempotency:" namespaces them away from other Redis data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String IN_FLIGHT_SENTINEL = "__PROCESSING__";
    static final Duration KEY_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * Atomically claims an idempotency key for in-flight processing.
     *
     * @return true if this is a brand-new key (caller should process the request)
     */
    public boolean claimKey(String idempotencyKey) {
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key(idempotencyKey), IN_FLIGHT_SENTINEL, KEY_TTL);
        return Boolean.TRUE.equals(claimed);
    }

    /** Stores the JSON response body under the key, replacing the PROCESSING sentinel. */
    public void storeResponse(String idempotencyKey, String responseJson) {
        redisTemplate.opsForValue().set(key(idempotencyKey), responseJson, KEY_TTL);
        log.debug("Stored idempotency response for key={}", idempotencyKey);
    }

    /**
     * Returns the cached response body if a previous call completed successfully.
     * Empty if the key is absent or still in-flight.
     */
    public Optional<String> getCachedResponse(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(key(idempotencyKey));
        if (value != null && !IN_FLIGHT_SENTINEL.equals(value)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /** Returns true if the key exists but is still being processed by another request. */
    public boolean isInFlight(String idempotencyKey) {
        return IN_FLIGHT_SENTINEL.equals(redisTemplate.opsForValue().get(key(idempotencyKey)));
    }

    /** Releases the in-flight claim (called if the request fails, so clients can retry). */
    public void releaseKey(String idempotencyKey) {
        redisTemplate.delete(key(idempotencyKey));
    }

    private static String key(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }
}
