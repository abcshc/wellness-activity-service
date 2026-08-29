package io.github.abcshc.wellnessactivity.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.member.error.MemberErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

	@Test
	void 오류_코드의_HTTP_상태와_외부_코드를_보존한다() {
		BusinessException exception = new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);

		assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		assertThat(exception.getErrorCode().httpStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.getErrorCode().code()).isEqualTo("MEMBER_EMAIL_ALREADY_EXISTS");
	}
}
