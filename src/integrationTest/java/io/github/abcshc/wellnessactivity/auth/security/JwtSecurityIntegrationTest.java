package io.github.abcshc.wellnessactivity.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.token.AccessToken;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import({JwtSecurityIntegrationTest.ProtectedResourceController.class, MySqlTestContainerConfiguration.class})
class JwtSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenIssuer jwtTokenIssuer;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void Bearer_Token_없이_보호_경로에_접근하면_공통_401_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/protected-resource"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
			.andExpect(jsonPath("$.path").value("/api/v1/protected-resource"));
	}

	@Test
	void 유효한_Bearer_Token은_인증을_통과해_보호_경로까지_전달된다() throws Exception {
		AccessToken accessToken = jwtTokenIssuer.issue(1L);

		mockMvc.perform(get("/api/v1/protected-resource")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.value()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subject").value("1"));
	}

	@Test
	void 위조된_Bearer_Token은_공통_401_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/protected-resource")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 다른_발행자의_서명된_Bearer_Token은_공통_401_응답을_반환한다() throws Exception {
		String token = issueToken("https://another-issuer.test");

		mockMvc.perform(get("/api/v1/protected-resource")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 만료된_Bearer_Token은_공통_401_응답을_반환한다() throws Exception {
		Instant expiredAt = Instant.now().minusSeconds(120);
		String token = issueToken(
			"https://wellness-activity-service.test",
			expiredAt.minusSeconds(60),
			expiredAt
		);

		mockMvc.perform(get("/api/v1/protected-resource")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private String issueToken(String issuer) {
		Instant issuedAt = Instant.now();
		return issueToken(issuer, issuedAt, issuedAt.plusSeconds(60));
	}

	private String issueToken(String issuer, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject("1")
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(),
			claims
		)).getTokenValue();
	}

	@RestController
	static class ProtectedResourceController {

		@GetMapping("/api/v1/protected-resource")
		AuthSubjectResponse protectedResource(@AuthenticationPrincipal Jwt jwt) {
			return new AuthSubjectResponse(jwt.getSubject());
		}
	}

	private record AuthSubjectResponse(String subject) {
	}
}
