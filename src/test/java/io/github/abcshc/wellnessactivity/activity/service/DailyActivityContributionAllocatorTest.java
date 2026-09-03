package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchInsert;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DailyActivityContributionAllocatorTest {

	private final DailyActivityContributionAllocator allocator = new DailyActivityContributionAllocator();

	@Test
	void KST_자정_경계_이벤트를_시간_비율로_일별_배분한다() {
		List<DailyActivityContribution> contributions = allocator.allocate(record(
			"2024-11-14T14:30:00Z", "2024-11-14T15:30:00Z", "100", "2", "10", "4"
		));

		assertThat(contributions).containsExactly(
			contribution("2024-11-14", "50", "1", "5", "2"),
			contribution("2024-11-15", "50", "1", "5", "2")
		);
	}

	@Test
	void 길이가_0인_이벤트는_시작한_KST_날짜에_전량을_귀속한다() {
		List<DailyActivityContribution> contributions = allocator.allocate(record(
			"2024-11-14T15:00:00Z", "2024-11-14T15:00:00Z", "12", "0.12", "0", "1.2"
		));

		assertThat(contributions).containsExactly(
			contribution("2024-11-15", "12", "0.12", "0", "1.2")
		);
	}

	@Test
	void 여러_KST_날짜를_가로지르는_이벤트는_각_날짜의_겹친_시간만큼_배분한다() {
		List<DailyActivityContribution> contributions = allocator.allocate(record(
			"2024-11-14T03:00:00Z", "2024-11-16T03:00:00Z", "48", "4.8", "9.6", "4.8"
		));

		assertThat(contributions).containsExactly(
			contribution("2024-11-14", "12", "1.2", "2.4", "1.2"),
			contribution("2024-11-15", "24", "2.4", "4.8", "2.4"),
			contribution("2024-11-16", "12", "1.2", "2.4", "1.2")
		);
	}

	@Test
	void 여러_신규_이벤트의_같은_KST_날짜_기여값을_병합한다() {
		Map<LocalDate, DailyActivityContribution> contributions = allocator.allocateAndMerge(List.of(
			record("2024-11-14T15:00:00Z", "2024-11-14T15:10:00Z", "100", "0.8", "5", "0"),
			record("2024-11-14T15:10:00Z", "2024-11-14T15:20:00Z", "50", "0.4", "0", "2")
		));

		assertThat(contributions).containsExactly(
			Map.entry(LocalDate.of(2024, 11, 15), contribution("2024-11-15", "150", "1.2", "5", "2"))
		);
	}

	@Test
	void 마지막_날짜에는_소수점_잔여값을_배정해_원본_합계를_보존한다() {
		List<DailyActivityContribution> contributions = allocator.allocate(record(
			"2024-11-14T14:40:00Z", "2024-11-14T15:40:00Z", "10", "1", "1", "0"
		));

		assertThat(contributions).extracting(DailyActivityContribution::steps)
			.containsExactly(decimal("3.33333333333333333333"), decimal("6.66666666666666666667"));
		assertThat(contributions.stream().map(DailyActivityContribution::steps).reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo("10");
	}

	private StepRecordBatchInsert record(
		String startedAt,
		String endedAt,
		String steps,
		String distanceKm,
		String sourceCaloriesKcal,
		String estimatedCaloriesKcal
	) {
		return new StepRecordBatchInsert(
			Instant.parse(startedAt), Instant.parse(endedAt), decimal(steps), decimal(distanceKm),
			decimal(sourceCaloriesKcal), decimal(estimatedCaloriesKcal), null
		);
	}

	private DailyActivityContribution contribution(
		String activityDate,
		String steps,
		String distanceKm,
		String sourceCaloriesKcal,
		String estimatedCaloriesKcal
	) {
		return new DailyActivityContribution(
			LocalDate.parse(activityDate), decimal(steps), decimal(distanceKm), decimal(sourceCaloriesKcal),
			decimal(estimatedCaloriesKcal)
		);
	}

	private BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
