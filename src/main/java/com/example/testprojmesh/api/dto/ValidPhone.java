package com.example.testprojmesh.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Documented
@Constraint(validatedBy = ValidPhone.Validator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {

    String message() default "must contain 10 to 15 digits and only digits, spaces, +, -, ( )";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPhone, String> {

        private static final Pattern ALLOWED = Pattern.compile("^\\+?[\\d\\s\\-()]+$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true; // absence is @NotBlank's job, so an optional filter can use this too
            }
            if (!ALLOWED.matcher(value).matches()) {
                return false;
            }
            int digits = value.replaceAll("\\D", "").length();
            return digits >= 10 && digits <= 15;
        }
    }
}
