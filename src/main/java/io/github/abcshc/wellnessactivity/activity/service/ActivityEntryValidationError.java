package io.github.abcshc.wellnessactivity.activity.service;

public record ActivityEntryValidationError(
	int index,
	String field,
	String code,
	String message
) {
}
