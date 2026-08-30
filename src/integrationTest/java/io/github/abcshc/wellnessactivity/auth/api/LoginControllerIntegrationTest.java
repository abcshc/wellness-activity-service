package io.github.abcshc.wellnessactivity.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
class LoginControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void setUp() {
		memberRepository.save(new MemberEntity("홍길동", "길동이", "login@example.com", passwordHasher.hash("password")));
	}

	@Test
	void 유효한_로그인_요청에_토큰_쌍을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
			{"email":"LOGIN@example.com","password":"password"}
			"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.accessTokenExpiresAt").isNotEmpty());
	}

	@Test
	void 잘못된_자격증명은_동일한_401_오류를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
			{"email":"unknown@example.com","password":"password"}
			"""))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
			{"email":"login@example.com","password":"wrong"}
			"""))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void 누락된_값과_잘못된_이메일_형식은_400_오류를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
			{"email":"invalid-email","password":""}
			"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").isNotEmpty())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").isNotEmpty());
	}
}
