package io.github.abcshc.wellnessactivity.member.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BCrypt가 신규 비밀번호에서 안전하게 처리하는 UTF-8 기준 72바이트 상한을 검증한다.
 */
@Documented
@Constraint(validatedBy = PasswordMaxBytesValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface PasswordMaxBytes {

	String message() default "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
