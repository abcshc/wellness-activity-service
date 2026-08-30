package io.github.abcshc.wellnessactivity.auth.token;

import io.github.abcshc.wellnessactivity.auth.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer {

	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public AccessToken issue(Long memberId) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_TTL);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.subject(memberId.toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(value, issuedAt, expiresAt);
	}
}
