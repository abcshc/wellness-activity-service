package io.github.abcshc.wellnessactivity.auth.token;

import java.time.Instant;

public record AccessToken(
	String value,
	Instant issuedAt,
	Instant expiresAt
) {
}
