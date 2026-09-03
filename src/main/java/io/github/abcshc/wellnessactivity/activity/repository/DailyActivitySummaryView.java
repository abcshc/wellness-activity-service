package io.github.abcshc.wellnessactivity.activity.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyActivitySummaryView(
	LocalDate activityDate,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal sourceCaloriesKcal,
	BigDecimal estimatedCaloriesKcal
) {
}
