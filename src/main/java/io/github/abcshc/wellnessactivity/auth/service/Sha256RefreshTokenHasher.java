package io.github.abcshc.wellnessactivity.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class Sha256RefreshTokenHasher implements RefreshTokenHasher {

	@Override
	public String hash(String refreshToken) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
				.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
