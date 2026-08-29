package io.github.abcshc.wellnessactivity.member.service;

import java.nio.charset.StandardCharsets;
import io.github.abcshc.wellnessactivity.member.policy.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

	private final PasswordEncoder passwordEncoder;

	public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public String hash(String rawPassword) {
		validatePasswordByteLength(rawPassword);
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		validatePasswordByteLength(rawPassword);
		return passwordEncoder.matches(rawPassword, passwordHash);
	}

	private void validatePasswordByteLength(String rawPassword) {
		if (rawPassword.getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_RAW_PASSWORD_BYTES) {
			throw new IllegalArgumentException("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
		}
	}
}
