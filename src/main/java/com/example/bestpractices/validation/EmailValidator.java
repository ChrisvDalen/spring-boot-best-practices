package com.example.bestpractices.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Best practices demonstrated:
 * - Implements ConstraintValidator<Annotation, ValueType> — stateless by design
 * - Compile the regex once as a static final (not on every call)
 * - Treat null as valid; pair with @NotBlank when null is also unacceptable
 *   (avoids coupling two constraints into one)
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    // RFC-5321 simplified pattern — covers the vast majority of real email addresses
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return EMAIL_PATTERN.matcher(value).matches();
    }
}
