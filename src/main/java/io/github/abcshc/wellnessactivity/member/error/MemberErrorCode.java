package io.github.abcshc.wellnessactivity.member.error;

import io.github.abcshc.wellnessactivity.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {

	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER_EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	MemberErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus httpStatus() {
		return httpStatus;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String message() {
		return message;
	}
}
