package io.github.abcshc.wellnessactivity.auth.api;

import io.github.abcshc.wellnessactivity.auth.service.RefreshTokenResult;
import java.time.Instant;

public record RefreshTokenResponse(
	String accessToken,
	String refreshToken,
	Instant accessTokenExpiresAt
) {
	public static RefreshTokenResponse from(RefreshTokenResult result) {
		return new RefreshTokenResponse(
			result.accessToken().value(),
			result.refreshToken(),
			result.accessToken().expiresAt()
		);
	}
}
