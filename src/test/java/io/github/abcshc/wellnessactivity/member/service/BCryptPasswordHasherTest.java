package io.github.abcshc.wellnessactivity.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherTest {

	private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

	@Test
	void 평문과_다른_60자_BCrypt_해시를_생성하고_검증한다() {
		String rawPassword = "password";

		String passwordHash = passwordHasher.hash(rawPassword);

		assertThat(passwordHash)
			.isNotEqualTo(rawPassword)
			.hasSize(60);
		assertThat(passwordHasher.matches(rawPassword, passwordHash)).isTrue();
		assertThat(passwordHasher.matches("wrong-password", passwordHash)).isFalse();
	}

	@Test
	void UTF8_기준_73바이트_비밀번호는_거부한다() {
		String rawPassword = "가".repeat(24) + "a";

		assertThatIllegalArgumentException()
			.isThrownBy(() -> passwordHasher.hash(rawPassword))
			.withMessage("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
	}

	@Test
	void UTF8_기준_72바이트_비밀번호는_해시할_수_있다() {
		String rawPassword = "가".repeat(24);

		String passwordHash = passwordHasher.hash(rawPassword);

		assertThat(passwordHasher.matches(rawPassword, passwordHash)).isTrue();
	}
}
