package io.github.abcshc.wellnessactivity.activity.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyActivityContribution(
	LocalDate activityDate,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal sourceCaloriesKcal,
	BigDecimal estimatedCaloriesKcal
) {

	public DailyActivityContribution add(DailyActivityContribution other) {
		if (!activityDate.equals(other.activityDate)) {
			throw new IllegalArgumentException("서로 다른 활동일의 기여값은 합칠 수 없습니다.");
		}
		return new DailyActivityContribution(
			activityDate,
			normalize(steps.add(other.steps)),
			normalize(distanceKm.add(other.distanceKm)),
			normalize(sourceCaloriesKcal.add(other.sourceCaloriesKcal)),
			normalize(estimatedCaloriesKcal.add(other.estimatedCaloriesKcal))
		);
	}

	static DailyActivityContribution of(
		LocalDate activityDate,
		BigDecimal steps,
		BigDecimal distanceKm,
		BigDecimal sourceCaloriesKcal,
		BigDecimal estimatedCaloriesKcal
	) {
		return new DailyActivityContribution(
			activityDate,
			normalize(steps),
			normalize(distanceKm),
			normalize(sourceCaloriesKcal),
			normalize(estimatedCaloriesKcal)
		);
	}

	private static BigDecimal normalize(BigDecimal value) {
		BigDecimal normalized = value.stripTrailingZeros();
		return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
	}
}
