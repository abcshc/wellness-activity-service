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
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
			.andExpect(jsonPath("$[1].caloriesKcal").value(1.21))
			.andExpect(jsonPath("$[1].sourceCaloriesKcal").doesNotExist())
			.andExpect(jsonPath("$[1].estimatedCaloriesKcal").doesNotExist())
			.andExpect(jsonPath("$[1].caloriesEstimateVersion").doesNotExist());
	}

	@Test
	void 원천_칼로리가_0이면_일별_조회에_걸음수_기반_추정값을_포함한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");
		MemberActivityKeyEntity activityKey = savedActivityKey(member, "record-key-001");
		stepRecordRepository.saveAndFlush(new StepRecordEntity(
			activityKey,
			ActivityProvider.APPLE_HEALTH,
			Instant.parse("2024-11-15T00:00:00Z"),
			Instant.parse("2024-11-15T00:10:00Z"),
			new BigDecimal("100"),
			new BigDecimal("0.08"),
			BigDecimal.ZERO,
			new BigDecimal("4"),
			"STEP_COUNT_V1"
		));

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-15")
				.param("to", "2024-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].caloriesKcal").value(4))
			.andExpect(jsonPath("$[0].sourceCaloriesKcal").doesNotExist())
			.andExpect(jsonPath("$[0].estimatedCaloriesKcal").doesNotExist())
			.andExpect(jsonPath("$[0].caloriesEstimateVersion").doesNotExist());
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
	void 최대_366일_범위는_빈_날짜를_포함해_연속된_일별_버킷을_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");
		MemberActivityKeyEntity activityKey = savedActivityKey(member, "record-key-001");
		stepRecordRepository.saveAndFlush(new StepRecordEntity(
			activityKey,
			ActivityProvider.HEALTH_CONNECT,
			Instant.parse("2024-11-14T15:00:00Z"),
			Instant.parse("2024-11-14T15:10:00Z"),
			new BigDecimal("32"),
			new BigDecimal("0.024"),
			new BigDecimal("1.2")
		));

		ResultActions response = mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-15")
				.param("to", "2025-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(366))
			.andExpect(jsonPath("$[0].date").value("2024-11-15"))
			.andExpect(jsonPath("$[0].steps").value(32))
			.andExpect(jsonPath("$[1].date").value("2024-11-16"))
			.andExpect(jsonPath("$[1].steps").value(0))
			.andExpect(jsonPath("$[365].date").value("2025-11-15"))
			.andExpect(jsonPath("$[365].steps").value(0));

		expectDailyBucketDates(response, LocalDate.parse("2024-11-15"), 366);
	}

	@Test
	void 최대_24개월_범위는_빈_월을_포함해_연속된_월별_버킷을_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");
		MemberActivityKeyEntity activityKey = savedActivityKey(member, "record-key-001");
		stepRecordRepository.saveAndFlush(new StepRecordEntity(
			activityKey,
			ActivityProvider.HEALTH_CONNECT,
			Instant.parse("2023-12-31T15:00:00Z"),
			Instant.parse("2023-12-31T15:10:00Z"),
			new BigDecimal("50"),
			new BigDecimal("0.04"),
			new BigDecimal("2")
		));

		ResultActions response = mockMvc.perform(get("/api/v1/activities/steps/monthly")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-01")
				.param("to", "2025-12"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(24))
			.andExpect(jsonPath("$[0].month").value("2024-01"))
			.andExpect(jsonPath("$[0].steps").value(50))
			.andExpect(jsonPath("$[1].month").value("2024-02"))
			.andExpect(jsonPath("$[1].steps").value(0))
			.andExpect(jsonPath("$[23].month").value("2025-12"))
			.andExpect(jsonPath("$[23].steps").value(0));

		expectMonthlyBucketLabels(response, YearMonth.parse("2024-01"), 24);
	}

	@Test
	void 일별_367일_조회_범위는_범위_초과_오류를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-01-01")
				.param("to", "2025-01-01"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ACTIVITY_DAILY_RANGE_TOO_LARGE"));
	}

	@Test
	void 월별_25개월_조회_범위는_범위_초과_오류를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(get("/api/v1/activities/steps/monthly")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.param("recordkey", "record-key-001")
				.param("from", "2024-01")
				.param("to", "2026-01"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ACTIVITY_MONTHLY_RANGE_TOO_LARGE"));
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

	@Test
	void 존재하지_않는_회원의_유효_JWT로_조회하면_401_오류를_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenIssuer.issue(999L).value())
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-15")
				.param("to", "2024-11-15"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
			.andExpect(jsonPath("$.path").value("/api/v1/activities/steps/daily"));
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

	private void expectDailyBucketDates(ResultActions response, LocalDate from, int size) throws Exception {
		for (int index = 0; index < size; index++) {
			response.andExpect(jsonPath("$[%d].date".formatted(index)).value(from.plusDays(index).toString()));
		}
	}

	private void expectMonthlyBucketLabels(ResultActions response, YearMonth from, int size) throws Exception {
		for (int index = 0; index < size; index++) {
			response.andExpect(jsonPath("$[%d].month".formatted(index)).value(from.plusMonths(index).toString()));
		}
	}
}
