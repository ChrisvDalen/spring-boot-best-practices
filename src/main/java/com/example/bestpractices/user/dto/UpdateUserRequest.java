package com.example.bestpractices.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Best practices demonstrated:
 * - Separate DTO for updates — fields are optional (null = no change)
 * - Using @Size without @NotBlank allows callers to send partial updates
 *   without forcing them to repeat unchanged fields
 */
@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    private Boolean active;
}
