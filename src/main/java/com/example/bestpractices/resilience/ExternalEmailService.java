package com.example.bestpractices.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Best practices demonstrated:
 * - @CircuitBreaker opens the circuit after N failures and stops calling the
 *   downstream service, giving it time to recover (fail-fast pattern)
 * - @Retry with exponential back-off handles transient failures transparently
 *   without burdening callers with retry logic
 * - @TimeLimiter enforces a hard timeout on async calls so a slow provider cannot
 *   drain the thread pool; combined with CompletableFuture for non-blocking execution
 * - Fallback methods share the same signature + a Throwable parameter; Resilience4j
 *   routes to the appropriate fallback based on exception type
 * - All configuration is externalised to application.yml (resilience4j.*) — no
 *   magic numbers in source code, easy to tune per environment
 */
@Slf4j
@Service
public class ExternalEmailService {

    private static final String CB_NAME = "emailService";

    /**
     * Sends a welcome email. Protected by circuit breaker + retry + timeout.
     * Returns a CompletableFuture so @TimeLimiter can enforce the timeout without
     * blocking a platform thread.
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "sendWelcomeEmailFallback")
    @Retry(name = CB_NAME)
    @TimeLimiter(name = CB_NAME)
    public CompletableFuture<Void> sendWelcomeEmailAsync(String email, String name) {
        return CompletableFuture.runAsync(() -> {
            log.info("Sending welcome email to {} ({})", name, email);
            // In production: HTTP call to SendGrid / Mailgun / AWS SES
            doSendEmail(email, name);
        });
    }

    /** Convenience wrapper so callers need not handle CompletableFuture. */
    public void sendWelcomeEmail(String email, String name) {
        if (email == null) return;
        try {
            sendWelcomeEmailAsync(email, name).get();
        } catch (Exception ex) {
            // Fallback already logged; swallow here so consumer processing continues
            log.debug("sendWelcomeEmail suppressed exception after fallback: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Void> sendWelcomeEmailFallback(String email, String name,
                                                              Throwable cause) {
        log.warn("Email service unavailable for {} — queuing for later. Cause: {}",
                email, cause.getMessage());
        // Could push to a retry-later queue or persist to a scheduled_emails table
        return CompletableFuture.completedFuture(null);
    }

    private void doSendEmail(String email, String name) {
        // Placeholder for actual HTTP call to email provider
        log.debug("Email delivery simulated for {}", email);
    }
}
