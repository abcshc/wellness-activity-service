package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchInsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DailyActivityContributionAllocator {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
	private static final int ALLOCATION_SCALE = 20;

	public List<DailyActivityContribution> allocate(StepRecordBatchInsert record) {
		Instant startedAt = record.startedAtUtc();
		Instant endedAt = record.endedAtUtc();
		if (startedAt.equals(endedAt)) {
			return List.of(contribution(
				activityDateOf(startedAt),
				record.steps(), record.distanceKm(), record.caloriesKcal(), record.estimatedCaloriesKcal()
			));
		}

		BigDecimal totalDuration = BigDecimal.valueOf(Duration.between(startedAt, endedAt).toNanos());
		BigDecimal remainingSteps = record.steps();
		BigDecimal remainingDistance = record.distanceKm();
		BigDecimal remainingSourceCalories = record.caloriesKcal();
		BigDecimal remainingEstimatedCalories = record.estimatedCaloriesKcal();
		List<DailyActivityContribution> contributions = new ArrayList<>();

		LocalDate activityDate = activityDateOf(startedAt);
		Instant dayStart = startOfDay(activityDate);
		while (dayStart.isBefore(endedAt)) {
			Instant nextDayStart = startOfDay(activityDate.plusDays(1));
			Instant overlapStart = startedAt.isAfter(dayStart) ? startedAt : dayStart;
			Instant overlapEnd = endedAt.isBefore(nextDayStart) ? endedAt : nextDayStart;
			boolean isLastContribution = !nextDayStart.isBefore(endedAt);

			BigDecimal steps = isLastContribution
				? remainingSteps : allocateValue(record.steps(), overlapStart, overlapEnd, totalDuration);
			BigDecimal distance = isLastContribution
				? remainingDistance : allocateValue(record.distanceKm(), overlapStart, overlapEnd, totalDuration);
			BigDecimal sourceCalories = isLastContribution
				? remainingSourceCalories : allocateValue(record.caloriesKcal(), overlapStart, overlapEnd, totalDuration);
			BigDecimal estimatedCalories = isLastContribution
				? remainingEstimatedCalories : allocateValue(record.estimatedCaloriesKcal(), overlapStart, overlapEnd, totalDuration);

			contributions.add(contribution(activityDate, steps, distance, sourceCalories, estimatedCalories));
			remainingSteps = remainingSteps.subtract(steps);
			remainingDistance = remainingDistance.subtract(distance);
			remainingSourceCalories = remainingSourceCalories.subtract(sourceCalories);
			remainingEstimatedCalories = remainingEstimatedCalories.subtract(estimatedCalories);
			activityDate = activityDate.plusDays(1);
			dayStart = nextDayStart;
		}
		return contributions;
	}

	public Map<LocalDate, DailyActivityContribution> allocateAndMerge(List<StepRecordBatchInsert> records) {
		Map<LocalDate, DailyActivityContribution> contributions = new LinkedHashMap<>();
		for (StepRecordBatchInsert record : records) {
			for (DailyActivityContribution contribution : allocate(record)) {
				contributions.merge(contribution.activityDate(), contribution, DailyActivityContribution::add);
			}
		}
		return contributions;
	}

	private BigDecimal allocateValue(
		BigDecimal value,
		Instant overlapStart,
		Instant overlapEnd,
		BigDecimal totalDuration
	) {
		BigDecimal overlapDuration = BigDecimal.valueOf(Duration.between(overlapStart, overlapEnd).toNanos());
		return value.multiply(overlapDuration).divide(totalDuration, ALLOCATION_SCALE, RoundingMode.HALF_UP);
	}

	private DailyActivityContribution contribution(
		LocalDate activityDate,
		BigDecimal steps,
		BigDecimal distanceKm,
		BigDecimal sourceCaloriesKcal,
		BigDecimal estimatedCaloriesKcal
	) {
		return DailyActivityContribution.of(
			activityDate, steps, distanceKm, sourceCaloriesKcal, estimatedCaloriesKcal
		);
	}

	private LocalDate activityDateOf(Instant instant) {
		return instant.atZone(BUSINESS_ZONE).toLocalDate();
	}

	private Instant startOfDay(LocalDate activityDate) {
		// 활동일의 경계는 원본 UTC 날짜가 아니라 서비스 기준 시간대의 자정이다.
		return activityDate.atStartOfDay(BUSINESS_ZONE).toInstant();
	}
}
