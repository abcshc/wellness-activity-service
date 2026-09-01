package io.github.abcshc.wellnessactivity.activity.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyActivitySummary(
	LocalDate date,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal
) {
}
