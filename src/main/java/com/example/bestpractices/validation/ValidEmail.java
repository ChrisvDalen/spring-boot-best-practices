package com.example.bestpractices.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Best practices demonstrated:
 * - Custom constraint annotation keeps validation logic reusable and co-located
 * - @Documented makes the constraint visible in generated Javadoc
 * - Default message follows the Bean Validation convention (class FQCN + .message)
 *   so it can be overridden in ValidationMessages.properties
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    String message() default "must be a valid email address";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
