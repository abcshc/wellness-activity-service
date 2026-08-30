package io.github.abcshc.wellnessactivity.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.error.MemberErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerIntegrationTest.ErrorTestController.class)
class GlobalExceptionHandlerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 비즈니스_예외를_일관된_오류_응답으로_반환한다() throws Exception {
		mockMvc.perform(get("/test/errors/business").with(user("test")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.code").value("MEMBER_EMAIL_ALREADY_EXISTS"))
			.andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
			.andExpect(jsonPath("$.path").value("/test/errors/business"))
			.andExpect(jsonPath("$.timestamp").exists())
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void 예상하지_못한_예외의_내부_메시지를_노출하지_않는다() throws Exception {
		mockMvc.perform(get("/test/errors/unexpected")
				.with(user("test"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("처리 중 알 수 없는 오류가 발생했습니다."))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("internal detail")));
	}

	@RestController
	static class ErrorTestController {

		@GetMapping("/test/errors/business")
		void throwBusinessException() {
			throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		}

		@GetMapping("/test/errors/unexpected")
		void throwUnexpectedException() {
			throw new IllegalStateException("internal detail");
		}
	}
}
