package io.github.abcshc.wellnessactivity.auth.service;

public interface RefreshTokenHasher {

	String hash(String refreshToken);
}
