package io.github.abcshc.wellnessactivity.member.api.validation;

import io.github.abcshc.wellnessactivity.member.policy.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordMaxBytesValidator implements ConstraintValidator<PasswordMaxBytes, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || value.getBytes(StandardCharsets.UTF_8).length <= PasswordPolicy.MAX_RAW_PASSWORD_BYTES;
	}
}
