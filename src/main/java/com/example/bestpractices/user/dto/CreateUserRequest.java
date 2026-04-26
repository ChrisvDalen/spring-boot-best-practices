package com.example.bestpractices.user.dto;

import com.example.bestpractices.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Best practices demonstrated:
 * - Separate request/response DTOs from the JPA entity; never expose entities directly
 * - Declare constraints on the DTO, not the entity, for API-layer validation
 * - Use @NotBlank (not @NotNull) for strings — rejects blank whitespace strings too
 */
@Data
public class CreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @ValidEmail
    private String email;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;
}
