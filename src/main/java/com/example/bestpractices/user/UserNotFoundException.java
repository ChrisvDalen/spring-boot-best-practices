package com.example.bestpractices.user;

/**
 * Best practices demonstrated:
 * - Domain-specific unchecked exception; callers don't need to declare or catch it
 * - Carries the offending ID so the global handler can produce a useful error message
 *   without the service layer needing to know about HTTP
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
