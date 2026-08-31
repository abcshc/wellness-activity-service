package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.token.AccessToken;

public record RefreshTokenResult(
	AccessToken accessToken,
	String refreshToken
) {
}
