package io.github.abcshc.wellnessactivity.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.service.LoginCommand;
import io.github.abcshc.wellnessactivity.auth.service.LoginResult;
import io.github.abcshc.wellnessactivity.auth.service.LoginService;
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
class RefreshTokenControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private LoginService loginService;

	@BeforeEach
	void setUp() {
		memberRepository.save(new MemberEntity("홍길동", "길동이", "refresh-api@example.com", passwordHasher.hash("password")));
	}

	@Test
	void 유효한_Refresh_Token으로_새_토큰_쌍을_반환한다() throws Exception {
		LoginResult loginResult = loginService.login(new LoginCommand("refresh-api@example.com", "password"));

		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
			{"refreshToken":"%s"}
			""".formatted(loginResult.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(loginResult.refreshToken())))
			.andExpect(jsonPath("$.accessTokenExpiresAt").isNotEmpty());
	}

	@Test
	void 존재하지_않는_Refresh_Token은_일반적인_401_오류를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
			{"refreshToken":"unknown-token"}
			"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));
	}

}
