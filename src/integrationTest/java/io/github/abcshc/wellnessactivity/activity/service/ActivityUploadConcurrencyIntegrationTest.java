package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadConcurrencyIntegrationTest {

	private static final int RECORD_COUNT = 201;
	private static final LocalDate SUMMARY_DATE = LocalDate.of(2025, 2, 1);
	private static final YearMonth SUMMARY_MONTH = YearMonth.of(2025, 2);

	@Autowired
	private ActivityUploadService activityUploadService;

	@Autowired
	private ActivitySummaryService activitySummaryService;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		stepRecordRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 동일_201건_묶음을_동시에_재전송해도_한번만_저장하고_집계는_중복되지_않는다() throws Exception {
		MemberEntity member = savedMember("same@example.com", "동일");
		ActivityInputNormalizationResult input = input("record-key-same", "1", "0.1", "1");

		List<ActivityUploadResult> results = runConcurrently(
			() -> activityUploadService.upload(member.getId(), input),
			() -> activityUploadService.upload(member.getId(), input)
		);

		assertThat(results.stream().mapToInt(ActivityUploadResult::createdCount).sum()).isEqualTo(RECORD_COUNT);
		assertThat(results.stream().mapToInt(ActivityUploadResult::ignoredCount).sum()).isEqualTo(RECORD_COUNT);
		assertThat(stepRecordRepository.count()).isEqualTo(RECORD_COUNT);

		MemberActivityKeyEntity key = memberActivityKeyRepository.findByRecordKey("record-key-same").orElseThrow();
		assertSummaries(key, "201", "20.1", "201");
	}

	@Test
	void 서로_다른_회원과_recordkey의_201건_신규_업로드는_서로_분리된다() throws Exception {
		MemberEntity firstMember = savedMember("first@example.com", "첫째");
		MemberEntity secondMember = savedMember("second@example.com", "둘째");

		List<ActivityUploadResult> results = runConcurrently(
			() -> activityUploadService.upload(firstMember.getId(), input("record-key-first", "1", "0.1", "1")),
			() -> activityUploadService.upload(secondMember.getId(), input("record-key-second", "2", "0.2", "2"))
		);

		assertThat(results).extracting(ActivityUploadResult::createdCount).containsOnly(RECORD_COUNT);
		assertThat(results).extracting(ActivityUploadResult::ignoredCount).containsOnly(0);
		assertThat(stepRecordRepository.count()).isEqualTo(RECORD_COUNT * 2L);

		assertSummaries(memberActivityKeyRepository.findByRecordKey("record-key-first").orElseThrow(), "201", "20.1", "201");
		assertSummaries(memberActivityKeyRepository.findByRecordKey("record-key-second").orElseThrow(), "402", "40.2", "402");
	}

	@SafeVarargs
	private final List<ActivityUploadResult> runConcurrently(Callable<ActivityUploadResult>... tasks) throws Exception {
		CountDownLatch ready = new CountDownLatch(tasks.length);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(tasks.length);
		try {
			List<Future<ActivityUploadResult>> futures = java.util.Arrays.stream(tasks)
				.map(task -> executorService.submit(() -> {
					ready.countDown();
					assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
					return task.call();
				}))
				.toList();
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			return futures.stream().map(this::resultOf).toList();
		} finally {
			executorService.shutdownNow();
		}
	}

	private ActivityUploadResult resultOf(Future<ActivityUploadResult> future) {
		try {
			return future.get(15, TimeUnit.SECONDS);
		} catch (Exception exception) {
			throw new IllegalStateException("동시 업로드 작업이 완료되지 않았습니다.", exception);
		}
	}

	private void assertSummaries(MemberActivityKeyEntity key, String steps, String distance, String calories) {
		assertThat(activitySummaryService.summarizeDaily(key, SUMMARY_DATE, SUMMARY_DATE)).containsExactly(
			new DailyActivitySummary(SUMMARY_DATE, decimal(steps), decimal(distance), decimal(calories))
		);
		assertThat(activitySummaryService.summarizeMonthly(key, SUMMARY_MONTH, SUMMARY_MONTH)).containsExactly(
			new MonthlyActivitySummary(SUMMARY_MONTH, decimal(steps), decimal(distance), decimal(calories))
		);
	}

	private MemberEntity savedMember(String email, String nickname) {
		return memberRepository.saveAndFlush(new MemberEntity("동시테스트", nickname, email, "password-hash"));
	}

	private ActivityInputNormalizationResult input(String recordKey, String steps, String distanceKm, String caloriesKcal) {
		List<NormalizedStepRecordCommand> records = IntStream.range(0, RECORD_COUNT)
			.mapToObj(index -> record(index, steps, distanceKm, caloriesKcal))
			.toList();
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(recordKey, ActivityProvider.SAMSUNG_HEALTH, records),
			List.of()
		);
	}

	private NormalizedStepRecordCommand record(int index, String steps, String distanceKm, String caloriesKcal) {
		Instant startedAt = Instant.parse("2025-02-01T00:00:00Z").plusSeconds(index);
		return new NormalizedStepRecordCommand(
			startedAt, startedAt.plusSeconds(1), decimal(steps), decimal(distanceKm), decimal(caloriesKcal)
		);
	}

	private BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
