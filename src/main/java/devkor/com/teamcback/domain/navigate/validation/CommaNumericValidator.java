package devkor.com.teamcback.domain.navigate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CommaNumericValidator implements ConstraintValidator<CommaNumeric, String> {
    @Override
    public void initialize(CommaNumeric constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return value.matches("[0-9,]+");
    }
}
