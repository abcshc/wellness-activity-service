package io.github.abcshc.wellnessactivity.member.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class PasswordMaxBytesValidatorTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void 한글_24자는_UTF8_기준_72바이트로_유효하다() {
		assertThat(validator.validate(new PasswordInput("가".repeat(24)))).isEmpty();
	}

	@Test
	void 한글_24자와_영문_한글자는_UTF8_기준_73바이트로_유효하지_않다() {
		assertThat(validator.validate(new PasswordInput("가".repeat(24) + "a")))
			.singleElement()
			.extracting(error -> error.getMessage())
			.isEqualTo("최대 72바이트입니다.");
	}

	private record PasswordInput(
		@NotBlank
		@PasswordMaxBytes(message = "최대 72바이트입니다.")
		String password
	) {
	}
}
