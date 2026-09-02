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
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadBatchStorageContractIntegrationTest {

	private static final Instant FIRST_ACTIVITY_AT = Instant.parse("2025-02-01T00:00:00Z");

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

	@ParameterizedTest(name = "{0}건 신규 활동을 저장한다")
	@ValueSource(ints = {100, 101, 200, 201, 1_000})
	void 청크_경계와_대량_신규_활동의_생성_건수는_최종_행_수와_같다(int recordCount) {
		MemberEntity member = savedMember();

		ActivityUploadResult result = activityUploadService.upload(
			member.getId(), input("record-key-" + recordCount, recordCount)
		);

		assertThat(result.totalCount()).isEqualTo(recordCount);
		assertThat(result.createdCount()).isEqualTo(recordCount);
		assertThat(result.ignoredCount()).isZero();
		assertThat(result.invalidCount()).isZero();
		assertThat(stepRecordRepository.count()).isEqualTo(recordCount);
	}

	@ParameterizedTest(name = "{0}건 동일 활동을 재전송하면 다시 저장하지 않는다")
	@ValueSource(ints = {101, 201, 1_000})
	void 동일_묶음을_재전송하면_모든_항목을_무시하고_최종_행_수는_변하지_않는다(int recordCount) {
		MemberEntity member = savedMember();
		ActivityInputNormalizationResult input = input("record-key-retry-" + recordCount, recordCount);

		ActivityUploadResult first = activityUploadService.upload(member.getId(), input);
		ActivityUploadResult retry = activityUploadService.upload(member.getId(), input);

		assertThat(first.createdCount()).isEqualTo(recordCount);
		assertThat(retry.createdCount()).isZero();
		assertThat(retry.ignoredCount()).isEqualTo(recordCount);
		assertThat(stepRecordRepository.count()).isEqualTo(recordCount);
	}

	@ParameterizedTest(name = "{0}건 기존 활동과 {0}건 신규 활동이 섞인 요청을 저장한다")
	@ValueSource(ints = {100, 101, 200})
	void 신규와_중복_활동이_섞인_요청은_신규_항목만_생성한다(int halfCount) {
		MemberEntity member = savedMember();
		String recordKey = "record-key-mixed-" + halfCount;
		activityUploadService.upload(member.getId(), input(recordKey, halfCount));

		ActivityUploadResult result = activityUploadService.upload(
			member.getId(), input(recordKey, halfCount * 2)
		);

		assertThat(result.totalCount()).isEqualTo(halfCount * 2);
		assertThat(result.createdCount()).isEqualTo(halfCount);
		assertThat(result.ignoredCount()).isEqualTo(halfCount);
		assertThat(stepRecordRepository.count()).isEqualTo(halfCount * 2);
	}

	private MemberEntity savedMember() {
		return memberRepository.saveAndFlush(new MemberEntity(
			"배치테스트", "배치", "batch@example.com", "password-hash"
		));
	}

	private ActivityInputNormalizationResult input(String recordKey, int recordCount) {
		List<NormalizedStepRecordCommand> records = IntStream.range(0, recordCount)
			.mapToObj(this::record)
			.toList();
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(recordKey, ActivityProvider.SAMSUNG_HEALTH, records),
			List.of()
		);
	}

	private NormalizedStepRecordCommand record(int index) {
		Instant startedAt = FIRST_ACTIVITY_AT.plusSeconds(index * 600L);
		return new NormalizedStepRecordCommand(
			startedAt,
			startedAt.plusSeconds(600),
			BigDecimal.valueOf(20L + index % 180),
			new BigDecimal("0.08"),
			new BigDecimal("3.20")
		);
	}
}
