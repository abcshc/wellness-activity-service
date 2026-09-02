package io.github.abcshc.wellnessactivity.activity.service;

import java.math.BigDecimal;

final class StepCaloriesEstimator {

	private static final BigDecimal KCAL_PER_STEP = new BigDecimal("0.04");

	StepCaloriesEstimate estimate(BigDecimal steps, BigDecimal sourceCaloriesKcal) {
		// 원천값이 없는 일상 걸음 활동에만 적용하는 사용자 참고용 fallback이다.
		if (sourceCaloriesKcal.signum() > 0) {
			return new StepCaloriesEstimate(BigDecimal.ZERO, null);
		}
		return new StepCaloriesEstimate(steps.multiply(KCAL_PER_STEP), "STEP_COUNT_V1");
	}

	record StepCaloriesEstimate(BigDecimal caloriesKcal, String version) {
	}
}
