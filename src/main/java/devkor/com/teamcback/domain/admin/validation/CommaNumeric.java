package devkor.com.teamcback.domain.admin.validation;

import jakarta.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CommaNumericValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CommaNumeric {
    String message() default "숫자와 콤마로만 이루어져야 합니다.";

    Class[] groups() default {};

    Class[] payload() default {};
}