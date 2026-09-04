package io.github.abcshc.wellnessactivity.activity.entity;

import java.util.Arrays;
import java.util.Optional;

public enum ActivityProvider {

	SAMSUNG_HEALTH("SamsungHealth"),
	APPLE_HEALTH("Health Kit"),
	HEALTH_CONNECT("HealthConnect");

	private final String sourceName;

	ActivityProvider(String sourceName) {
		this.sourceName = sourceName;
	}

	public static Optional<ActivityProvider> fromSourceName(String sourceName) {
		return Arrays.stream(values())
			.filter(provider -> provider.sourceName.equals(sourceName))
			.findFirst();
	}
}
