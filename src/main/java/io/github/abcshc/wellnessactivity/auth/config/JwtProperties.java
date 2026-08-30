package io.github.abcshc.wellnessactivity.auth.config;

import jakarta.validation.constraints.NotBlank;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security.jwt")
@Validated
public record JwtProperties(
	@NotBlank(message = "JWT 발행자는 필수입니다.")
	String issuer,
	@NotBlank(message = "JWT 서명 키는 필수입니다.")
	String secret
) {

	public SecretKey secretKey() {
		byte[] decodedSecret;
		try {
			decodedSecret = Base64.getDecoder().decode(secret);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("JWT 서명 키는 Base64 형식이어야 합니다.", exception);
		}

		if (decodedSecret.length < 32) {
			throw new IllegalArgumentException("JWT 서명 키는 256비트 이상이어야 합니다.");
		}

		return new SecretKeySpec(decodedSecret, "HmacSHA256");
	}
}
