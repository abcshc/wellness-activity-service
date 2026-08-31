package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
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

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadServiceIntegrationTest {

	@Autowired
	private ActivityUploadService activityUploadService;

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
	void 동일_이벤트를_재전송해도_원본은_한번만_저장한다() {
		MemberEntity member = savedMember();

		ActivityUploadResult first = activityUploadService.upload(member, normalizedInput());
		ActivityUploadResult retry = activityUploadService.upload(member, normalizedInput());

		assertThat(first.createdCount()).isEqualTo(1);
		assertThat(retry.createdCount()).isZero();
		assertThat(retry.ignoredCount()).isEqualTo(1);
		assertThat(stepRecordRepository.count()).isEqualTo(1);
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
			return activityUploadService.upload(member, normalizedInput());
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
}
