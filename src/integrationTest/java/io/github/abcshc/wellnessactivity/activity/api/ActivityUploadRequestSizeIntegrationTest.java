package io.github.abcshc.wellnessactivity.activity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadRequestSizeIntegrationTest {

	private static final Instant FIRST_ACTIVITY_AT = Instant.parse("2025-01-01T00:00:00Z");
	private static final DateTimeFormatter SAMSUNG_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	@ParameterizedTest(name = "{0} {1} 항목을 한 요청으로 저장한다")
	@MethodSource("requestSizes")
	void 현재_제한_없이_100_500_1000건_활동을_한번에_저장한다(
		String sourceName,
		int entryCount,
		boolean offsetIncluded
	) throws Exception {
		MemberEntity member = memberRepository.saveAndFlush(new MemberEntity(
			"부하테스트", "부하", "load-" + entryCount + "@example.com", "password-hash"
		));
		ActivityUploadRequest request = request(sourceName, entryCount, offsetIncluded);

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenIssuer.issue(member.getId()).value())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCount").value(entryCount))
			.andExpect(jsonPath("$.createdCount").value(entryCount))
			.andExpect(jsonPath("$.ignoredCount").value(0))
			.andExpect(jsonPath("$.invalidCount").value(0));

		assertThat(stepRecordRepository.count()).isEqualTo(entryCount);
	}

	private static Stream<org.junit.jupiter.params.provider.Arguments> requestSizes() {
		return Stream.of(
			org.junit.jupiter.params.provider.Arguments.of("SamsungHealth", 100, false),
			org.junit.jupiter.params.provider.Arguments.of("Health Kit", 500, true),
			org.junit.jupiter.params.provider.Arguments.of("HealthConnect", 1_000, true)
		);
	}

	private ActivityUploadRequest request(String sourceName, int entryCount, boolean offsetIncluded) {
		List<ActivityEntryRequest> entries = IntStream.range(0, entryCount)
			.mapToObj(index -> entry(index, offsetIncluded))
			.toList();
		return new ActivityUploadRequest(
			"request-size-" + sourceName.toLowerCase().replace(" ", "-") + "-" + entryCount,
			new ActivityDataRequest(entries, new ActivitySourceRequest(sourceName)),
			"steps"
		);
	}

	private ActivityEntryRequest entry(int index, boolean offsetIncluded) {
		Instant startedAt = FIRST_ACTIVITY_AT.plusSeconds(index * 600L);
		Instant endedAt = startedAt.plusSeconds(600);
		return new ActivityEntryRequest(
			new ActivityPeriodRequest(format(startedAt, offsetIncluded), format(endedAt, offsetIncluded)),
			BigDecimal.valueOf(20L + index % 180),
			new ActivityMeasureRequest("km", new BigDecimal("0.08")),
			new ActivityMeasureRequest("kcal", new BigDecimal("3.20"))
		);
	}

	private String format(Instant value, boolean offsetIncluded) {
		if (offsetIncluded) {
			return value.toString().replace("Z", "+0000");
		}
		return LocalDateTime.ofInstant(value, ZoneOffset.UTC).format(SAMSUNG_TIME_FORMATTER);
	}
}
