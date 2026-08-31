package io.github.abcshc.wellnessactivity.activity.api;

import java.math.BigDecimal;

public record ActivityMeasureRequest(
	String unit,
	BigDecimal value
) {
}
