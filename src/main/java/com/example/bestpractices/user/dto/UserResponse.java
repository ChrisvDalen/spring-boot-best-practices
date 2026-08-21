package com.example.bestpractices.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Best practices demonstrated:
 * - @Value + @Builder makes the response immutable and easy to construct
 * - @Jacksonized generates @JsonDeserialize(builder=...) + @JsonPOJOBuilder(withPrefix="")
 *   on the Lombok-generated builder, enabling Jackson to deserialise this immutable class
 *   — required for Redis cache which serialises values as JSON via GenericJacksonJsonRedisSerializer
 * - Expose only what the API consumer needs — omit sensitive or internal fields
 * - Use Instant for timestamps; clients can format them in their own timezone
 */
@Value
@Builder
@Jacksonized
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
