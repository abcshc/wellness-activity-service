package io.github.abcshc.wellnessactivity.member.service;

public record MemberRegistrationCommand(
	String name,
	String nickname,
	String email,
	String password
) {
}
