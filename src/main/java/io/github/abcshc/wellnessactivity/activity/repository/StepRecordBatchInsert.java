package io.github.abcshc.wellnessactivity.activity.repository;

import java.math.BigDecimal;
import java.time.Instant;

public record StepRecordBatchInsert(
	Instant startedAtUtc,
	Instant endedAtUtc,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal,
	BigDecimal estimatedCaloriesKcal,
	String caloriesEstimateVersion
) {
}
