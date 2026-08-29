package io.github.abcshc.wellnessactivity.common.web;

public record FieldErrorResponse(
	String field,
	String message
) {
}
