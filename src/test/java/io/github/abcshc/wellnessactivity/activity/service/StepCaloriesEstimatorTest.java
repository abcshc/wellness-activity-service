package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StepCaloriesEstimatorTest {

	private final StepCaloriesEstimator estimator = new StepCaloriesEstimator();

	@Test
	void 원천_칼로리가_0이면_걸음당_004kcal을_추정한다() {
		StepCaloriesEstimator.StepCaloriesEstimate estimate = estimator.estimate(new BigDecimal("1250"), BigDecimal.ZERO);

		assertThat(estimate.caloriesKcal()).isEqualByComparingTo("50.00");
		assertThat(estimate.version()).isEqualTo("STEP_COUNT_V1");
	}

	@Test
	void 원천_칼로리가_있으면_추정하지_않는다() {
		StepCaloriesEstimator.StepCaloriesEstimate estimate = estimator.estimate(
			new BigDecimal("1250"), new BigDecimal("45.5")
		);

		assertThat(estimate.caloriesKcal()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(estimate.version()).isNull();
	}

	@Test
	void 걸음수가_0이면_원천_칼로리가_0이어도_추정_규칙을_기록하지_않는다() {
		StepCaloriesEstimator.StepCaloriesEstimate estimate = estimator.estimate(BigDecimal.ZERO, BigDecimal.ZERO);

		assertThat(estimate.caloriesKcal()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(estimate.version()).isNull();
	}
}
