package io.github.abcshc.wellnessactivity.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	@Test
	void Base64로_인코딩된_256비트_이상_비밀키를_생성한다() {
		JwtProperties properties = new JwtProperties(
			"https://wellness-activity-service.test",
			"MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="
		);

		assertThat(properties.secretKey().getEncoded()).hasSize(32);
	}

	@Test
	void 이백오십육비트보다_짧은_비밀키는_거부한다() {
		JwtProperties properties = new JwtProperties(
			"https://wellness-activity-service.test",
			"MTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
		);

		assertThatThrownBy(properties::secretKey)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("JWT 서명 키는 256비트 이상이어야 합니다.");
	}

	@Test
	void Base64_형식이_아닌_비밀키는_거부한다() {
		JwtProperties properties = new JwtProperties(
			"https://wellness-activity-service.test",
			"not-base64"
		);

		assertThatThrownBy(properties::secretKey)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("JWT 서명 키는 Base64 형식이어야 합니다.");
	}
}
