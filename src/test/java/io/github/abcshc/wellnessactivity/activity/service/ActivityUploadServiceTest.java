package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryRepository;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchInsert;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchInsertResult;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ActivityUploadServiceTest {

	private final MemberActivityKeyRepository memberActivityKeyRepository = Mockito.mock(MemberActivityKeyRepository.class);
	private final StepRecordBatchRepository stepRecordBatchRepository = Mockito.mock(StepRecordBatchRepository.class);
	private final DailyActivityContributionAllocator dailyActivityContributionAllocator = new DailyActivityContributionAllocator();
	private final DailyActivitySummaryRepository dailyActivitySummaryRepository = Mockito.mock(DailyActivitySummaryRepository.class);
	private final ActivityUploadService activityUploadService = new ActivityUploadService(
		memberActivityKeyRepository,
		stepRecordBatchRepository,
		dailyActivityContributionAllocator,
		dailyActivitySummaryRepository
	);

	@Test
	void 유효_항목을_저장하고_같은_요청의_중복_항목은_한번만_보존한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(stepRecordBatchRepository.insertIgnore(any(), any(), any())).thenReturn(batchResult(1));

		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			List.of(recordAt(0), recordAt(0)),
			List.of(new ActivityEntryValidationError(2, "steps", "ACTIVITY_INVALID_STEPS", "걸음 수가 올바르지 않습니다."))
		));

		verify(stepRecordBatchRepository).insertIgnore(any(), any(), any());
		verify(memberActivityKeyRepository).insertIgnore(1L, "record-key-001");
		verify(dailyActivitySummaryRepository).upsert(
			any(), eq(LocalDate.of(2024, 11, 15)), eq(new BigDecimal("32")),
			eq(new BigDecimal("0.02422")), eq(new BigDecimal("1.21")), eq(BigDecimal.ZERO)
		);
		assertThat(result.totalCount()).isEqualTo(3);
		assertThat(result.createdCount()).isEqualTo(1);
		assertThat(result.ignoredCount()).isEqualTo(1);
		assertThat(result.invalidCount()).isEqualTo(1);
		assertThat(result.invalidEntries()).hasSize(1);
	}

	@Test
	void 이미_저장된_같은_활동은_원본을_유지하고_무시한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(stepRecordBatchRepository.insertIgnore(any(), any(), any())).thenReturn(batchResult(0));

		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(List.of(recordAt(0)), List.of()));

		assertThat(result.createdCount()).isZero();
		assertThat(result.ignoredCount()).isEqualTo(1);
		verifyNoInteractions(dailyActivitySummaryRepository);
	}

	@Test
	void 활동_201건은_200건과_1건의_청크로_나누어_저장한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(stepRecordBatchRepository.insertIgnore(any(), any(), any())).thenReturn(batchResult(200), batchResult(1));

		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			IntStream.range(0, 201).mapToObj(this::recordAt).toList(),
			List.of()
		));

		ArgumentCaptor<List<StepRecordBatchInsert>> inserts = listCaptor();
		verify(stepRecordBatchRepository, times(2)).insertIgnore(any(), any(), inserts.capture());
		assertThat(inserts.getAllValues()).extracting(List::size).containsExactly(200, 1);
		assertThat(result.createdCount()).isEqualTo(201);
	}

	@Test
	void 다른_회원에게_연결된_recordkey는_저장할_수_없다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));

		assertThatThrownBy(() -> activityUploadService.upload(2L, normalizedInput(List.of(recordAt(0)), List.of())))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.RECORD_KEY_FORBIDDEN)
			);
		verify(stepRecordBatchRepository, never()).insertIgnore(any(), any(), any());
	}

	@Test
	void 유효한_항목이_없으면_recordkey를_연결하거나_저장하지_않는다() {
		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			List.of(),
			List.of(new ActivityEntryValidationError(0, "period.to", "ACTIVITY_INVALID_PERIOD", "활동 기간이 올바르지 않습니다."))
		));

		verify(memberActivityKeyRepository, never()).findByRecordKey(any());
		verify(memberActivityKeyRepository, never()).insertIgnore(any(), any());
		verify(stepRecordBatchRepository, never()).insertIgnore(any(), any(), any());
		assertThat(result.totalCount()).isEqualTo(1);
		assertThat(result.invalidCount()).isEqualTo(1);
	}

	@Test
	void 원천_칼로리가_0인_새_활동은_추정값과_규칙_버전을_함께_저장한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(stepRecordBatchRepository.insertIgnore(any(), any(), any())).thenReturn(batchResult(1));

		activityUploadService.upload(1L, normalizedInput(List.of(new NormalizedStepRecordCommand(
			Instant.parse("2024-11-15T00:00:00Z"),
			Instant.parse("2024-11-15T00:10:00Z"),
			new BigDecimal("100"), new BigDecimal("0.08"), BigDecimal.ZERO
		)), List.of()));

		ArgumentCaptor<List<StepRecordBatchInsert>> inserts = listCaptor();
		verify(stepRecordBatchRepository).insertIgnore(any(), any(), inserts.capture());
		assertThat(inserts.getValue()).singleElement().satisfies(insert -> {
			assertThat(insert.estimatedCaloriesKcal()).isEqualByComparingTo("4");
			assertThat(insert.caloriesEstimateVersion()).isEqualTo("STEP_COUNT_V1");
		});
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<StepRecordBatchInsert>> listCaptor() {
		return ArgumentCaptor.forClass(List.class);
	}

	private ActivityInputNormalizationResult normalizedInput(
		List<NormalizedStepRecordCommand> records,
		List<ActivityEntryValidationError> invalidEntries
	) {
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand("record-key-001", ActivityProvider.SAMSUNG_HEALTH, records),
			invalidEntries
		);
	}

	private NormalizedStepRecordCommand recordAt(int index) {
		Instant startedAtUtc = Instant.parse("2024-11-15T00:00:00Z").plusSeconds(index * 600L);
		return new NormalizedStepRecordCommand(
			startedAtUtc,
			startedAtUtc.plusSeconds(600),
			new BigDecimal("32"),
			new BigDecimal("0.02422"),
			new BigDecimal("1.21")
		);
	}

	private MemberActivityKeyEntity activityKey(Long memberId) {
		MemberEntity member = Mockito.mock(MemberEntity.class);
		when(member.getId()).thenReturn(memberId);
		return new MemberActivityKeyEntity(member, "record-key-001");
	}

	private StepRecordBatchInsertResult batchResult(int createdCount) {
		return new StepRecordBatchInsertResult(
			IntStream.range(0, createdCount).mapToObj(this::batchInsertAt).toList(),
			0
		);
	}

	private StepRecordBatchInsert batchInsertAt(int index) {
		NormalizedStepRecordCommand record = recordAt(index);
		return new StepRecordBatchInsert(
			record.startedAtUtc(), record.endedAtUtc(), record.steps(), record.distanceKm(), record.caloriesKcal(),
			BigDecimal.ZERO, null
		);
	}
}
