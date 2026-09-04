package io.github.abcshc.wellnessactivity.activity.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActivityProviderTest {

	@Test
	void 외부_원천_이름을_활동_원천으로_변환한다() {
		assertThat(ActivityProvider.fromSourceName("SamsungHealth"))
			.contains(ActivityProvider.SAMSUNG_HEALTH);
		assertThat(ActivityProvider.fromSourceName("Health Kit"))
			.contains(ActivityProvider.APPLE_HEALTH);
		assertThat(ActivityProvider.fromSourceName("HealthConnect"))
			.contains(ActivityProvider.HEALTH_CONNECT);
	}

	@Test
	void 알_수_없는_외부_원천_이름은_비어_있는_결과를_반환한다() {
		assertThat(ActivityProvider.fromSourceName("Unknown")).isEmpty();
	}
}
