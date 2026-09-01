package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.DailyActivitySummary;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyActivitySummaryResponse(
	String recordkey,
	LocalDate date,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal
) {

	public static DailyActivitySummaryResponse from(String recordKey, DailyActivitySummary summary) {
		return new DailyActivitySummaryResponse(
			recordKey, summary.date(), summary.steps(), summary.distanceKm(), summary.caloriesKcal()
		);
	}
}
