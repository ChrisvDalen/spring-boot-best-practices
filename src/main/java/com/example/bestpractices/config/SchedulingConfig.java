package com.example.bestpractices.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Best practices demonstrated:
 * - @EnableScheduling in a dedicated config class (not @SpringBootApplication) keeps
 *   the main class clean and makes it trivial to disable scheduling in test slices
 *   by excluding this config rather than reimporting the entire context
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
