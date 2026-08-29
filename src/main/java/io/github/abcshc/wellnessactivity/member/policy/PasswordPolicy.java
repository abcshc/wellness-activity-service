package io.github.abcshc.wellnessactivity.member.policy;

/**
 * BCrypt를 사용하는 회원 비밀번호의 입력 제약을 정의한다.
 */
public final class PasswordPolicy {

	public static final int MAX_RAW_PASSWORD_BYTES = 72;

	private PasswordPolicy() {
	}
}
