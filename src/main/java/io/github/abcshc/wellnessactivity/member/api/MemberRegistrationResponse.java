package io.github.abcshc.wellnessactivity.member.api;

import io.github.abcshc.wellnessactivity.member.service.MemberRegistrationResult;

public record MemberRegistrationResponse(
	String name,
	String nickname,
	String email
) {

	public static MemberRegistrationResponse from(MemberRegistrationResult result) {
		return new MemberRegistrationResponse(result.name(), result.nickname(), result.email());
	}
}
