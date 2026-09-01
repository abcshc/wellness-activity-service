package io.github.abcshc.wellnessactivity.activity.service;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyActivitySummary(
	YearMonth month,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal
) {
}
