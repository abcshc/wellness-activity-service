package io.github.abcshc.wellnessactivity.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.service.LoginCommand;
import io.github.abcshc.wellnessactivity.auth.service.LoginResult;
import io.github.abcshc.wellnessactivity.auth.service.LoginService;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class LogoutControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private LoginService loginService;

	@BeforeEach
	void setUp() {
		memberRepository.save(new MemberEntity("홍길동", "길동이", "logout@example.com", passwordHasher.hash("password")));
	}

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 로그아웃은_계열을_폐기하고_이후_갱신을_차단한다() throws Exception {
		LoginResult loginResult = loginService.login(new LoginCommand("logout@example.com", "password"));

		logout(loginResult.refreshToken()).andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
			{"refreshToken":"%s"}
			""".formatted(loginResult.refreshToken())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));
	}

	@Test
	void 이미_폐기됐거나_존재하지_않는_토큰도_204를_반환한다() throws Exception {
		LoginResult loginResult = loginService.login(new LoginCommand("logout@example.com", "password"));

		logout(loginResult.refreshToken()).andExpect(status().isNoContent());
		logout(loginResult.refreshToken()).andExpect(status().isNoContent());
		logout("unknown-token").andExpect(status().isNoContent());
	}

	private org.springframework.test.web.servlet.ResultActions logout(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("""
			{"refreshToken":"%s"}
			""".formatted(refreshToken)));
	}
}
