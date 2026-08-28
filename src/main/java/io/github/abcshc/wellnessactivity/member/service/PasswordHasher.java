package io.github.abcshc.wellnessactivity.member.service;

/**
 * 비밀번호 해시 알고리즘을 회원가입과 로그인 유스케이스에서 분리한다.
 */
public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String passwordHash);
}
