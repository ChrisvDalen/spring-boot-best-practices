package com.example.bestpractices.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Best practices demonstrated:
 * - @Async must be on a Spring-managed bean method called from OUTSIDE the same bean
 *   (self-invocation bypasses the proxy and runs synchronously)
 * - Return CompletableFuture<T> when callers need the result or error;
 *   return void only for true fire-and-forget tasks
 * - Exceptions in void @Async methods are caught by AsyncUncaughtExceptionHandler (see AsyncConfig)
 * - Use the named executor ("taskExecutor") to keep async work on a controlled thread pool
 */
@Slf4j
@Service
public class NotificationService {

    @Async("taskExecutor")
    public CompletableFuture<Boolean> sendWelcomeEmail(String email) {
        log.info("Sending welcome email to {}", email);
        try {
            // Simulate email sending latency
            Thread.sleep(200);
            log.info("Welcome email sent to {}", email);
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    // Fire-and-forget: no return value; exceptions handled by AsyncUncaughtExceptionHandler
    @Async("taskExecutor")
    public void sendDeactivationNotice(String email) {
        log.info("Sending deactivation notice to {}", email);
    }
}
