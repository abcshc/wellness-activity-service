package io.github.abcshc.wellnessactivity.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
class JwtTokenIssuerIntegrationTest {

	@Autowired
	private JwtTokenIssuer jwtTokenIssuer;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void 회원_ID를_subject로_담은_15분_만료_Access_Token을_발급한다() {
		AccessToken accessToken = jwtTokenIssuer.issue(1L);

		Jwt jwt = jwtDecoder.decode(accessToken.value());

		assertThat(jwt.getSubject()).isEqualTo("1");
		assertThat(jwt.getIssuer().toString()).isEqualTo("https://wellness-activity-service.test");
		assertThat(accessToken.expiresAt()).isAfter(accessToken.issuedAt());
	}
}
