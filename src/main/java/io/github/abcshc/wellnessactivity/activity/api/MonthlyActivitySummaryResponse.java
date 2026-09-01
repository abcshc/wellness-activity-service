package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.MonthlyActivitySummary;
import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyActivitySummaryResponse(
	String recordkey,
	YearMonth month,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal
) {

	public static MonthlyActivitySummaryResponse from(String recordKey, MonthlyActivitySummary summary) {
		return new MonthlyActivitySummaryResponse(
			recordKey, summary.month(), summary.steps(), summary.distanceKm(), summary.caloriesKcal()
		);
	}
}
