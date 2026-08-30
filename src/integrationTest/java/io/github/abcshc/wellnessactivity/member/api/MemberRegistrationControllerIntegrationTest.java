package io.github.abcshc.wellnessactivity.member.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
@Import(MySqlTestContainerConfiguration.class)
class MemberRegistrationControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 유효한_회원가입_요청에_비밀번호_없이_회원_정보를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동이",
					  "email": "GILDONG@EXAMPLE.COM",
					  "password": "password"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.nickname").value("길동이"))
			.andExpect(jsonPath("$.email").value("gildong@example.com"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void 필수값_누락과_잘못된_형식은_필드별_오류로_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "",
					  "nickname": "",
					  "email": "invalid-email",
					  "password": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.path").value("/api/v1/members"))
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").isNotEmpty())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'nickname')]").isNotEmpty())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").isNotEmpty())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").isNotEmpty());
	}

	@Test
	void 앞뒤_공백이_있는_이메일은_필드_오류로_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동이",
					  "email": " gildong@example.com ",
					  "password": "password"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").isNotEmpty());
	}

	@Test
	void UTF8_기준_73바이트_비밀번호는_필드_오류로_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동이",
					  "email": "gildong@example.com",
					  "password": "%s"
					}
					""".formatted("가".repeat(24) + "a")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')].message")
				.value("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."));
	}

	@Test
	void 중복된_이메일은_409_오류로_반환한다() throws Exception {
		String request = """
			{
			  "name": "홍길동",
			  "nickname": "길동이",
			  "email": "gildong@example.com",
			  "password": "password"
			}
			""";

		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_EMAIL_ALREADY_EXISTS"))
			.andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
	}
}
