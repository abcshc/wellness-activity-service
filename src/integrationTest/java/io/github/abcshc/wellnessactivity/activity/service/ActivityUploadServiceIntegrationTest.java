package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryRepository;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadServiceIntegrationTest {

	@Autowired
	private ActivityUploadService activityUploadService;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private DailyActivitySummaryRepository dailyActivitySummaryRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		stepRecordRepository.deleteAll();
		dailyActivitySummaryRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 동일_이벤트를_재전송해도_원본은_한번만_저장한다() {
		MemberEntity member = savedMember();

		ActivityUploadResult first = activityUploadService.upload(member.getId(), normalizedInput());
		ActivityUploadResult retry = activityUploadService.upload(member.getId(), normalizedInput());

		assertThat(first.createdCount()).isEqualTo(1);
		assertThat(retry.createdCount()).isZero();
		assertThat(retry.ignoredCount()).isEqualTo(1);
		assertThat(stepRecordRepository.count()).isEqualTo(1);
	}

	@Test
	void 원천_칼로리가_0인_활동은_원천값과_추정값을_분리해_저장한다() {
		MemberEntity member = savedMember();

		activityUploadService.upload(member.getId(), normalizedInputWithZeroCalories());

		var record = stepRecordRepository.findAll().get(0);
		assertThat(record.getCalories()).isEqualByComparingTo("0");
		assertThat(record.getEstimatedCalories()).isEqualByComparingTo("4");
		assertThat(record.getCaloriesEstimateVersion()).isEqualTo("STEP_COUNT_V1");
	}

	@Test
	void 신규_원본_이벤트만_KST_일별_집계에_반영하고_재전송은_집계를_늘리지_않는다() {
		MemberEntity member = savedMember();
		ActivityInputNormalizationResult input = input(
			"record-key-daily-summary",
			new NormalizedStepRecordCommand(
				Instant.parse("2024-11-14T14:30:00Z"),
				Instant.parse("2024-11-14T15:30:00Z"),
				new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("10")
			)
		);

		ActivityUploadResult first = activityUploadService.upload(member.getId(), input);
		ActivityUploadResult retry = activityUploadService.upload(member.getId(), input);
		Long activityKeyId = memberActivityKeyRepository.findByRecordKey("record-key-daily-summary").orElseThrow().getId();

		assertThat(first.createdCount()).isEqualTo(1);
		assertThat(retry.createdCount()).isZero();
		assertThat(dailySummaries(activityKeyId)).containsExactly(
			new DailySummaryRow(LocalDate.of(2024, 11, 14), decimal("50"), decimal("1"), decimal("5"), decimal("0")),
			new DailySummaryRow(LocalDate.of(2024, 11, 15), decimal("50"), decimal("1"), decimal("5"), decimal("0"))
		);
	}

	@Test
	void 같은_recordkey의_동시_업로드는_단일_인스턴스에서_한번만_저장한다() throws Exception {
		MemberEntity member = savedMember();
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		try {
			List<Future<ActivityUploadResult>> results = List.of(
				executorService.submit(uploadTask(member, ready, start)),
				executorService.submit(uploadTask(member, ready, start))
			);
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			int createdCount = 0;
			for (Future<ActivityUploadResult> result : results) {
				createdCount += result.get(10, TimeUnit.SECONDS).createdCount();
			}

			assertThat(createdCount).isEqualTo(1);
			assertThat(stepRecordRepository.count()).isEqualTo(1);
			assertThat(memberActivityKeyRepository.findByRecordKey("record-key-001")).isPresent();
		} finally {
			executorService.shutdownNow();
		}
	}

	private Callable<ActivityUploadResult> uploadTask(
		MemberEntity member,
		CountDownLatch ready,
		CountDownLatch start
	) {
		return () -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			return activityUploadService.upload(member.getId(), normalizedInput());
		};
	}

	private MemberEntity savedMember() {
		return memberRepository.saveAndFlush(new MemberEntity(
			"홍길동", "길동이", "member@example.com", "password-hash"
		));
	}

	private ActivityInputNormalizationResult normalizedInput() {
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(
				"record-key-001",
				ActivityProvider.SAMSUNG_HEALTH,
				List.of(new NormalizedStepRecordCommand(
					Instant.parse("2024-11-15T00:00:00Z"),
					Instant.parse("2024-11-15T00:10:00Z"),
					new BigDecimal("32"),
					new BigDecimal("0.02422"),
					new BigDecimal("1.21")
				))
			),
			List.of()
		);
	}

	private ActivityInputNormalizationResult normalizedInputWithZeroCalories() {
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(
				"record-key-001",
				ActivityProvider.APPLE_HEALTH,
				List.of(new NormalizedStepRecordCommand(
					Instant.parse("2024-11-15T00:00:00Z"),
					Instant.parse("2024-11-15T00:10:00Z"),
					new BigDecimal("100"), new BigDecimal("0.08"), BigDecimal.ZERO
				))
			),
			List.of()
		);
	}

	private ActivityInputNormalizationResult input(String recordKey, NormalizedStepRecordCommand... records) {
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(recordKey, ActivityProvider.SAMSUNG_HEALTH, List.of(records)),
			List.of()
		);
	}

	private List<DailySummaryRow> dailySummaries(Long activityKeyId) {
		return jdbcTemplate.query(
			"""
				select activity_date, steps, distance_km, source_calories_kcal, estimated_calories_kcal
				from daily_activity_summaries
				where member_activity_key_id = ?
				order by activity_date
				""",
			(resultSet, rowNum) -> new DailySummaryRow(
				resultSet.getObject("activity_date", LocalDate.class),
				resultSet.getBigDecimal("steps"),
				resultSet.getBigDecimal("distance_km"),
				resultSet.getBigDecimal("source_calories_kcal"),
				resultSet.getBigDecimal("estimated_calories_kcal")
			),
			activityKeyId
		);
	}

	private BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}

	private record DailySummaryRow(
		LocalDate activityDate,
		BigDecimal steps,
		BigDecimal distanceKm,
		BigDecimal sourceCaloriesKcal,
		BigDecimal estimatedCaloriesKcal
	) {
		private DailySummaryRow {
			steps = steps.stripTrailingZeros();
			distanceKm = distanceKm.stripTrailingZeros();
			sourceCaloriesKcal = sourceCaloriesKcal.stripTrailingZeros();
			estimatedCaloriesKcal = estimatedCaloriesKcal.stripTrailingZeros();
		}
	}
}
