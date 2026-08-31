package io.github.abcshc.wellnessactivity.activity.api;

import java.math.BigDecimal;

public record ActivityEntryRequest(
	ActivityPeriodRequest period,
	BigDecimal steps,
	ActivityMeasureRequest distance,
	ActivityMeasureRequest calories
) {
}
