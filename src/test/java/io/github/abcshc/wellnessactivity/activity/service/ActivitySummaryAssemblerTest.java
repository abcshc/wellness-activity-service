package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryRepository;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryView;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActivitySummaryAssemblerTest {

	private final DailyActivitySummaryRepository dailyActivitySummaryRepository = Mockito.mock(DailyActivitySummaryRepository.class);
	private final ActivitySummaryAssembler activitySummaryAssembler = new ActivitySummaryAssembler(dailyActivitySummaryRepository);

	@Test
	void 일별_집계_행을_조회하고_없는_날짜는_0으로_채운다() {
		when(dailyActivitySummaryRepository.findSummaries(any(), any(), any())).thenReturn(List.of(
			summary("2024-11-15", "150", "2", "16", "0")
		));

		List<DailyActivitySummary> result = activitySummaryAssembler.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		);

		assertThat(result).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("0"), decimal("0"), decimal("0")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("150"), decimal("2"), decimal("16"))
		);
		verify(dailyActivitySummaryRepository).findSummaries(
			any(), org.mockito.ArgumentMatchers.eq(LocalDate.of(2024, 11, 14)),
			org.mockito.ArgumentMatchers.eq(LocalDate.of(2024, 11, 15))
		);
	}

	@Test
	void 자정_경계의_기여값은_저장된_일별_집계를_그대로_반환한다() {
		when(dailyActivitySummaryRepository.findSummaries(any(), any(), any())).thenReturn(List.of(
			summary("2024-11-14", "50", "1", "5", "0"),
			summary("2024-11-15", "50", "1", "5", "0")
		));

		assertThat(activitySummaryAssembler.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 14), LocalDate.of(2024, 11, 15)
		)).containsExactly(
			new DailyActivitySummary(LocalDate.of(2024, 11, 14), decimal("50"), decimal("1"), decimal("5")),
			new DailyActivitySummary(LocalDate.of(2024, 11, 15), decimal("50"), decimal("1"), decimal("5"))
		);
	}

	@Test
	void 원천값과_추정값을_합산해_칼로리를_반환한다() {
		when(dailyActivitySummaryRepository.findSummaries(any(), any(), any())).thenReturn(List.of(
			summary("2024-11-15", "150", "0.12", "5", "2")
		));

		assertThat(activitySummaryAssembler.summarizeDaily(
			activityKey(), LocalDate.of(2024, 11, 15), LocalDate.of(2024, 11, 15)
		)).containsExactly(new DailyActivitySummary(
			LocalDate.of(2024, 11, 15), decimal("150"), decimal("0.12"), decimal("7")
		));
	}

	@Test
	void 월별_조회는_일별_집계를_한국_월로_합산하고_빈_월은_0으로_채운다() {
		when(dailyActivitySummaryRepository.findSummaries(any(), any(), any())).thenReturn(List.of(
			summary("2024-03-31", "10", "0.1", "1", "0"),
			summary("2024-04-01", "10", "0.1", "0", "1")
		));

		assertThat(activitySummaryAssembler.summarizeMonthly(
			activityKey(), YearMonth.of(2024, 3), YearMonth.of(2024, 5)
		)).containsExactly(
			new MonthlyActivitySummary(YearMonth.of(2024, 3), decimal("10"), decimal("0.1"), decimal("1")),
			new MonthlyActivitySummary(YearMonth.of(2024, 4), decimal("10"), decimal("0.1"), decimal("1")),
			new MonthlyActivitySummary(YearMonth.of(2024, 5), decimal("0"), decimal("0"), decimal("0"))
		);
		verify(dailyActivitySummaryRepository).findSummaries(
			any(), org.mockito.ArgumentMatchers.eq(LocalDate.of(2024, 3, 1)),
			org.mockito.ArgumentMatchers.eq(LocalDate.of(2024, 5, 31))
		);
	}

	private MemberActivityKeyEntity activityKey() {
		return new MemberActivityKeyEntity(
			new MemberEntity("홍길동", "길동이", "member@example.com", "password-hash"), "record-key-001"
		);
	}

	private DailyActivitySummaryView summary(
		String date, String steps, String distanceKm, String sourceCaloriesKcal, String estimatedCaloriesKcal
	) {
		return new DailyActivitySummaryView(
			LocalDate.parse(date), decimal(steps), decimal(distanceKm), decimal(sourceCaloriesKcal), decimal(estimatedCaloriesKcal)
		);
	}

	private BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
