package io.github.abcshc.wellnessactivity.auth.api;

import io.github.abcshc.wellnessactivity.auth.service.LoginResult;
import java.time.Instant;

public record LoginResponse(
	String accessToken,
	String refreshToken,
	Instant accessTokenExpiresAt
) {
	public static LoginResponse from(LoginResult result) {
		return new LoginResponse(
			result.accessToken().value(),
			result.refreshToken(),
			result.accessToken().expiresAt()
		);
	}
}
