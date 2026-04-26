package com.example.bestpractices.actuator;

import com.example.bestpractices.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Best practices demonstrated:
 * - Implement HealthIndicator to expose domain-level health beyond simple UP/DOWN
 * - Include actionable detail (e.g. user count) so ops can spot data-layer issues at a glance
 * - Keep the check cheap — no heavy queries, no external calls with long timeouts
 * - Exposed at /actuator/health; add management.endpoint.health.show-details=always
 *   in non-prod to see the details payload
 */
@Component
@RequiredArgsConstructor
public class AppHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    @Override
    public Health health() {
        try {
            long userCount = userRepository.count();
            return Health.up()
                    .withDetail("userCount", userCount)
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withException(ex)
                    .build();
        }
    }
}
