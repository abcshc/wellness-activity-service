package io.github.abcshc.wellnessactivity.activity.service;

import java.math.BigDecimal;
import java.time.Instant;

public record NormalizedStepRecordCommand(
	Instant startedAtUtc,
	Instant endedAtUtc,
	BigDecimal steps,
	BigDecimal distanceKm,
	BigDecimal caloriesKcal
) {
}
