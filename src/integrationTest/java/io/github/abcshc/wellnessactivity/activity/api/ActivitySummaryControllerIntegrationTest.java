package io.github.abcshc.wellnessactivity.activity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class ActivitySummaryControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenIssuer jwtTokenIssuer;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@AfterEach
	void tearDown() {
		stepRecordRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 본인의_일별_요약을_연속된_날짜와_함께_조회한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");
		MemberActivityKeyEntity activityKey = savedActivityKey(member, "record-key-001");
		stepRecordRepository.saveAndFlush(new StepRecordEntity(
			activityKey,
			ActivityProvider.SAMSUNG_HEALTH,
			Instant.parse("2024-11-15T00:00:00Z"),
			Instant.parse("2024-11-15T00:10:00Z"),
			new BigDecimal("32"),
			new BigDecimal("0.02422"),
			new BigDecimal("1.21")
		));

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-14")
				.param("to", "2024-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].recordkey").value("record-key-001"))
			.andExpect(jsonPath("$[0].date").value("2024-11-14"))
			.andExpect(jsonPath("$[0].steps").value(0))
			.andExpect(jsonPath("$[1].recordkey").value("record-key-001"))
			.andExpect(jsonPath("$[1].date").value("2024-11-15"))
			.andExpect(jsonPath("$[1].steps").value(32))
			.andExpect(jsonPath("$[1].distanceKm").value(0.02422))
			.andExpect(jsonPath("$[1].caloriesKcal").value(1.21));
	}

	@Test
	void 다른_회원의_recordkey는_조회할_수_없다() throws Exception {
		MemberEntity owner = savedMember("owner@example.com");
		savedActivityKey(owner, "record-key-001");
		MemberEntity otherMember = savedMember("other@example.com");

		mockMvc.perform(get("/api/v1/activities/steps/monthly")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(otherMember))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11")
				.param("to", "2024-11"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACTIVITY_RECORD_KEY_FORBIDDEN"));
	}

	@Test
	void 본인의_월별_요약을_조회한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");
		savedActivityKey(member, "record-key-001");

		mockMvc.perform(get("/api/v1/activities/steps/monthly")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11")
				.param("to", "2024-11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].recordkey").value("record-key-001"))
			.andExpect(jsonPath("$[0].month").value("2024-11"))
			.andExpect(jsonPath("$[0].steps").value(0));
	}

	@Test
	void 잘못된_조회_범위는_400_오류를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2025-01-01")
				.param("to", "2024-01-01"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ACTIVITY_INVALID_DAILY_RANGE"));
	}

	@Test
	void 인증되지_않은_조회는_401_오류를_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-15")
				.param("to", "2024-11-15"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private MemberEntity savedMember(String email) {
		return memberRepository.saveAndFlush(new MemberEntity("홍길동", "길동이", email, "password-hash"));
	}

	private MemberActivityKeyEntity savedActivityKey(MemberEntity member, String recordKey) {
		return memberActivityKeyRepository.saveAndFlush(new MemberActivityKeyEntity(member, recordKey));
	}

	private String bearerToken(MemberEntity member) {
		return "Bearer " + jwtTokenIssuer.issue(member.getId()).value();
	}
}
