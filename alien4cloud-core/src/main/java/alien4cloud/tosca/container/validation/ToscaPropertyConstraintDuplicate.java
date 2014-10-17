package alien4cloud.tosca.container.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ToscaPropertyConstraintDuplicateValidator.class)
@Documented
public @interface ToscaPropertyConstraintDuplicate {
    String message() default "{alien4cloud.tosca.container.validation.propertyconstraintduplicate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
