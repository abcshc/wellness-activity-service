package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActivityUploadServiceTest {

	private final MemberActivityKeyRepository memberActivityKeyRepository = Mockito.mock(MemberActivityKeyRepository.class);
	private final StepRecordRepository stepRecordRepository = Mockito.mock(StepRecordRepository.class);
	private final ActivityUploadService activityUploadService = new ActivityUploadService(
		memberActivityKeyRepository,
		stepRecordRepository
	);

	@Test
	void 유효_항목을_저장하고_같은_요청의_재전송은_한번만_보존한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(stepRecordRepository.insertIgnore(
			any(), any(), any(), any(), any(), any(), any()
		)).thenReturn(1, 0);

		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			List.of(record("2024-11-15T00:00:00Z"), record("2024-11-15T00:00:00Z")),
			List.of(new ActivityEntryValidationError(2, "steps", "ACTIVITY_INVALID_STEPS", "걸음 수가 올바르지 않습니다."))
		));

		verify(stepRecordRepository, times(2)).insertIgnore(any(), any(), any(), any(), any(), any(), any());
		verify(memberActivityKeyRepository).insertIgnore(1L, "record-key-001");
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
		when(stepRecordRepository.insertIgnore(
			any(), any(), any(), any(), any(), any(), any()
		)).thenReturn(0);

		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			List.of(record("2024-11-15T00:00:00Z")),
			List.of()
		));

		assertThat(result.createdCount()).isZero();
		assertThat(result.ignoredCount()).isEqualTo(1);
	}

	@Test
	void 다른_회원에게_연결된_recordkey는_저장할_수_없다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));

		assertThatThrownBy(() -> activityUploadService.upload(2L, normalizedInput(
			List.of(record("2024-11-15T00:00:00Z")), List.of()
		)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.RECORD_KEY_FORBIDDEN)
			);
		verify(stepRecordRepository, never()).insertIgnore(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void 유효한_항목이_없으면_recordkey를_연결하거나_저장하지_않는다() {
		ActivityUploadResult result = activityUploadService.upload(1L, normalizedInput(
			List.of(),
			List.of(new ActivityEntryValidationError(0, "period.to", "ACTIVITY_INVALID_PERIOD", "활동 기간이 올바르지 않습니다."))
		));

		verify(memberActivityKeyRepository, never()).findByRecordKey(any());
		verify(memberActivityKeyRepository, never()).insertIgnore(any(), any());
		verify(stepRecordRepository, never()).insertIgnore(any(), any(), any(), any(), any(), any(), any());
		assertThat(result.totalCount()).isEqualTo(1);
		assertThat(result.invalidCount()).isEqualTo(1);
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

	private NormalizedStepRecordCommand record(String startedAt) {
		Instant startedAtUtc = Instant.parse(startedAt);
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
}
