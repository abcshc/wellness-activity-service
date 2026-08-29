package io.github.abcshc.wellnessactivity.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.error.MemberErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

	@Test
	void 비즈니스_예외를_오류_코드에_정의된_HTTP_응답으로_변환한다() {
		MockHttpServletRequest request = request("/api/v1/members");

		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(
			new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody())
			.extracting(ErrorResponse::status, ErrorResponse::code, ErrorResponse::message, ErrorResponse::path)
			.containsExactly(409, "MEMBER_EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", "/api/v1/members");
		assertThat(response.getBody().fieldErrors()).isEmpty();
	}

	@Test
	void 예상하지_못한_예외는_내부_원인을_숨긴_오류_응답으로_변환한다() {
		MockHttpServletRequest request = request("/api/v1/members");

		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnexpectedException(
			new IllegalStateException("internal detail"),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody())
			.extracting(ErrorResponse::code, ErrorResponse::message)
			.containsExactly("INTERNAL_SERVER_ERROR", "처리 중 알 수 없는 오류가 발생했습니다.");
	}

	private MockHttpServletRequest request(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(path);
		return request;
	}
}
