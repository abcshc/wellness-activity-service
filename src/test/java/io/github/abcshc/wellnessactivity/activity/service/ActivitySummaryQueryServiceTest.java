package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActivitySummaryQueryServiceTest {

	private final MemberActivityKeyRepository memberActivityKeyRepository = Mockito.mock(MemberActivityKeyRepository.class);
	private final ActivitySummaryAssembler activitySummaryAssembler = Mockito.mock(ActivitySummaryAssembler.class);
	private final ActivitySummaryQueryService activitySummaryQueryService = new ActivitySummaryQueryService(
		memberActivityKeyRepository,
		activitySummaryAssembler
	);

	@Test
	void 소유한_recordkey의_일별_요약을_조회한다() {
		MemberActivityKeyEntity activityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001")).thenReturn(Optional.of(activityKey));
		when(activitySummaryAssembler.summarizeDaily(
			activityKey, LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		)).thenReturn(List.of(new DailyActivitySummary(
			LocalDate.of(2024, 11, 14), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO
		)));

		List<DailyActivitySummary> result = activitySummaryQueryService.daily(
			1L, "record-key-001", "2024-11-14", "2024-11-15"
		);

		assertThat(result).hasSize(1);
		verify(activitySummaryAssembler).summarizeDaily(
			activityKey, LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);
	}

	@Test
	void 소유하지_않았거나_없는_recordkey는_동일한_403_오류를_반환한다() {
		MemberActivityKeyEntity ownedActivityKey = activityKey(1L);
		when(memberActivityKeyRepository.findByRecordKey("record-key-001"))
			.thenReturn(Optional.of(ownedActivityKey));

		assertThatThrownBy(() -> activitySummaryQueryService.daily(
			2L, "record-key-001", "2024-11-14", "2024-11-15"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.RECORD_KEY_FORBIDDEN)
			);

		when(memberActivityKeyRepository.findByRecordKey("missing-key")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> activitySummaryQueryService.daily(
			1L, "missing-key", "2024-11-14", "2024-11-15"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.RECORD_KEY_FORBIDDEN)
			);
	}

	@Test
	void 잘못된_형식과_역전된_일별_범위는_400_오류를_반환한다() {
		assertThatThrownBy(() -> activitySummaryQueryService.daily(
			1L, "record-key-001", "2024/11/14", "2024-11-15"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.INVALID_DAILY_RANGE)
			);
		assertThatThrownBy(() -> activitySummaryQueryService.daily(
			1L, "record-key-001", "2024-11-16", "2024-11-15"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.INVALID_DAILY_RANGE)
			);
	}

	@Test
	void 일별은_366일_월별은_24개월을_초과해_조회할_수_없다() {
		assertThatThrownBy(() -> activitySummaryQueryService.daily(
			1L, "record-key-001", "2024-01-01", "2025-01-01"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.DAILY_RANGE_TOO_LARGE)
			);
		assertThatThrownBy(() -> activitySummaryQueryService.monthly(
			1L, "record-key-001", "2024-01", "2026-01"
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.MONTHLY_RANGE_TOO_LARGE)
			);
	}

	private MemberActivityKeyEntity activityKey(Long memberId) {
		MemberEntity member = Mockito.mock(MemberEntity.class);
		when(member.getId()).thenReturn(memberId);
		return new MemberActivityKeyEntity(member, "record-key-001");
	}
}
