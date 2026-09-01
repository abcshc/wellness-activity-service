package io.github.abcshc.wellnessactivity.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import({CurrentMemberIdArgumentResolverIntegrationTest.CurrentMemberController.class,
	MySqlTestContainerConfiguration.class})
class CurrentMemberIdArgumentResolverIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenIssuer jwtTokenIssuer;

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	void 유효한_회원_JWT는_컨트롤러에_회원_ID만_전달한다() throws Exception {
		MemberEntity member = memberRepository.saveAndFlush(
			new MemberEntity("홍길동", "길동이", "member@example.com", "password-hash")
		);

		mockMvc.perform(get("/api/v1/current-member-id")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenIssuer.issue(member.getId()).value()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(member.getId()));
	}

	@Test
	void 존재하지_않는_회원의_유효_JWT는_401_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/current-member-id")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenIssuer.issue(999L).value()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void JWT_없이_요청하면_401_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/current-member-id"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@RestController
	static class CurrentMemberController {

		@GetMapping("/api/v1/current-member-id")
		CurrentMemberIdResponse currentMemberId(@CurrentMemberId Long memberId) {
			return new CurrentMemberIdResponse(memberId);
		}
	}

	private record CurrentMemberIdResponse(Long memberId) {
	}
}
