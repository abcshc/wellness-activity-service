package io.github.abcshc.wellnessactivity.auth.service;

public record LoginCommand(
	String email,
	String password
) {
}
