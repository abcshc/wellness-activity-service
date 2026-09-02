package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActivitySummaryServiceTest {

	private final StepRecordRepository stepRecordRepository = Mockito.mock(StepRecordRepository.class);
	private final ActivitySummaryService activitySummaryService = new ActivitySummaryService(stepRecordRepository);

	@Test
	void 같은_한국_날짜의_원본_활동을_합산한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T15:00:00Z", "2024-11-14T16:00:00Z", "100", "1.5", "10.5"),
			record("2024-11-14T16:00:00Z", "2024-11-14T17:00:00Z", "50", "0.5", "5.5")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 15), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(new DailyActivitySummary(
			LocalDate.of(2024, 11, 15), decimal("150"), decimal("2"), decimal("16")
		));
	}

	@Test
	void 한국_자정_경계를_넘는_활동은_시간_비율로_나눈다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T14:30:00Z", "2024-11-14T15:30:00Z", "100", "2", "10")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("50"), decimal("1"), decimal("5")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("50"), decimal("1"), decimal("5"))
		);
	}

	@Test
	void 저장된_추정_칼로리는_날짜_경계에_맞춰_나눈다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T14:30:00Z", "2024-11-14T15:30:00Z", "100", "2", "0", "4")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("50"), decimal("1"), decimal("2")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("50"), decimal("1"), decimal("2"))
		);
	}

	@Test
	void 원천값과_저장된_추정값이_섞인_날짜는_최종_칼로리를_반환한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T15:00:00Z", "2024-11-14T15:10:00Z", "100", "0.08", "5"),
			record("2024-11-14T15:10:00Z", "2024-11-14T15:20:00Z", "50", "0.04", "0", "2")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 15), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(new DailyActivitySummary(
			LocalDate.of(2024, 11, 15), decimal("150"), decimal("0.12"), decimal("7")
		));
	}

	@Test
	void 비례_배분의_소수점_잔여값은_마지막_기간에_반영한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T14:40:00Z", "2024-11-14T15:40:00Z", "10", "1", "1")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);

		assertThat(result.get(0).steps()).isEqualByComparingTo("3.33333333333333333333");
		assertThat(result.get(1).steps()).isEqualByComparingTo("6.66666666666666666667");
		assertThat(result.get(0).steps().add(result.get(1).steps())).isEqualByComparingTo("10");
	}

	@Test
	void 한국_월_경계를_넘는_활동은_월별로_시간_비율을_반영한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-03-31T14:00:00Z", "2024-03-31T16:00:00Z", "20", "0.2", "2")
		));

		List<MonthlyActivitySummary> result = activitySummaryService.summarizeMonthly(
			activityKey(), YearMonth.of(2024, 3), YearMonth.of(2024, 4)
		);

		assertThat(result).containsExactly(
			new MonthlyActivitySummary(YearMonth.of(2024, 3), decimal("10"), decimal("0.1"), decimal("1")),
			new MonthlyActivitySummary(YearMonth.of(2024, 4), decimal("10"), decimal("0.1"), decimal("1"))
		);
	}

	@Test
	void 길이가_0인_활동은_시작한_한국_날짜에_전량을_귀속한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
			record("2024-11-14T15:00:00Z", "2024-11-14T15:00:00Z", "12", "0.12", "1.2")
		));

		List<DailyActivitySummary> result = activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("0"), decimal("0"), decimal("0")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("12"), decimal("0.12"), decimal("1.2"))
		);
	}

	@Test
	void 활동이_없는_날짜와_월도_0으로_반환한다() {
		when(stepRecordRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());

		assertThat(activitySummaryService.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		)).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("0"), decimal("0"), decimal("0")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("0"), decimal("0"), decimal("0"))
		);
		assertThat(activitySummaryService.summarizeMonthly(
			activityKey(), YearMonth.of(2024, 11), YearMonth.of(2024, 12)
		)).containsExactly(
			new MonthlyActivitySummary(YearMonth.of(2024, 11), decimal("0"), decimal("0"), decimal("0")),
			new MonthlyActivitySummary(YearMonth.of(2024, 12), decimal("0"), decimal("0"), decimal("0"))
		);
	}

	private MemberActivityKeyEntity activityKey() {
		return new MemberActivityKeyEntity(new MemberEntity("홍길동", "길동이", "member@example.com", "password-hash"), "record-key-001");
	}

	private StepRecordEntity record(String startedAt, String endedAt, String steps, String distance, String calories) {
		return new StepRecordEntity(
			activityKey(),
			ActivityProvider.SAMSUNG_HEALTH,
			Instant.parse(startedAt),
			Instant.parse(endedAt),
			decimal(steps),
			decimal(distance),
			decimal(calories)
		);
	}

	private StepRecordEntity record(
		String startedAt, String endedAt, String steps, String distance, String calories, String estimatedCalories
	) {
		return new StepRecordEntity(
			activityKey(), ActivityProvider.SAMSUNG_HEALTH, Instant.parse(startedAt), Instant.parse(endedAt),
			decimal(steps), decimal(distance), decimal(calories), decimal(estimatedCalories), "STEP_COUNT_V1"
		);
	}

	private BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
