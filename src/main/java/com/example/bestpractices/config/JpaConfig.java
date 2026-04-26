package com.example.bestpractices.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Best practices demonstrated:
 * - Enable JPA Auditing in a dedicated config class (not on the main application class)
 *   so it's easy to disable in slice tests with @DataJpaTest
 * - @CreatedDate / @LastModifiedDate on entities are populated automatically once this is active
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
