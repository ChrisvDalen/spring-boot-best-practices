package com.example.bestpractices.user.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Best practices demonstrated:
 * - @Value + @Builder makes the response immutable and easy to construct
 * - Expose only what the API consumer needs — omit sensitive or internal fields
 * - Use Instant for timestamps; clients can format them in their own timezone
 */
@Value
@Builder
public class UserResponse {
    Long id;
    String username;
    String email;
    String firstName;
    String lastName;
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}
