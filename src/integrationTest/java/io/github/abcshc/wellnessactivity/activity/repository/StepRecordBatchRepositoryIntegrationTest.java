package io.github.abcshc.wellnessactivity.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class StepRecordBatchRepositoryIntegrationTest {

	@Autowired
	private StepRecordBatchRepository stepRecordBatchRepository;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void MySQL_배치_저장은_실제_신규_항목만_입력_순서대로_반환한다() {
		MemberActivityKeyEntity activityKey = savedActivityKey();
		List<StepRecordBatchInsert> firstRequest = List.of(recordAt(0), recordAt(1));

		StepRecordBatchInsertResult firstResult = stepRecordBatchRepository.insertIgnore(
			activityKey.getId(), ActivityProvider.HEALTH_CONNECT, firstRequest
		);
		StepRecordBatchInsertResult retryResult = stepRecordBatchRepository.insertIgnore(
			activityKey.getId(), ActivityProvider.HEALTH_CONNECT, firstRequest
		);

		assertThat(firstResult.insertedRecords()).containsExactlyElementsOf(firstRequest);
		assertThat(firstResult.ignoredDuplicateCount()).isZero();
		assertThat(retryResult.insertedRecords()).isEmpty();
		assertThat(retryResult.ignoredDuplicateCount()).isEqualTo(2);
	}

	@Test
	void MySQL_배치_저장은_신규와_중복이_섞이면_신규_항목만_반환한다() {
		MemberActivityKeyEntity activityKey = savedActivityKey();
		List<StepRecordBatchInsert> existing = List.of(recordAt(0), recordAt(1));
		stepRecordBatchRepository.insertIgnore(activityKey.getId(), ActivityProvider.SAMSUNG_HEALTH, existing);
		List<StepRecordBatchInsert> mixedRequest = List.of(recordAt(1), recordAt(2), recordAt(0), recordAt(3));

		StepRecordBatchInsertResult result = stepRecordBatchRepository.insertIgnore(
			activityKey.getId(), ActivityProvider.SAMSUNG_HEALTH, mixedRequest
		);

		assertThat(result.insertedRecords()).containsExactly(recordAt(2), recordAt(3));
		assertThat(result.ignoredDuplicateCount()).isEqualTo(2);
		assertThat(stepRecordRepository.count()).isEqualTo(4);
	}

	private MemberActivityKeyEntity savedActivityKey() {
		MemberEntity member = memberRepository.saveAndFlush(new MemberEntity(
			"배치 계약", "계약", "batch-contract@example.com", "password-hash"
		));
		memberActivityKeyRepository.insertIgnore(member.getId(), "batch-contract-key");
		return memberActivityKeyRepository.findByRecordKey("batch-contract-key").orElseThrow();
	}

	private StepRecordBatchInsert recordAt(int index) {
		Instant startedAt = Instant.parse("2025-03-01T00:00:00Z").plusSeconds(index * 600L);
		return new StepRecordBatchInsert(
			startedAt,
			startedAt.plusSeconds(600),
			BigDecimal.valueOf(100L + index),
			new BigDecimal("0.10"),
			new BigDecimal("5.00"),
			BigDecimal.ZERO,
			null
		);
	}
}
