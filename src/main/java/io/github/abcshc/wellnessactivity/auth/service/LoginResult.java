package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.token.AccessToken;

public record LoginResult(
	AccessToken accessToken,
	String refreshToken
) {
}
