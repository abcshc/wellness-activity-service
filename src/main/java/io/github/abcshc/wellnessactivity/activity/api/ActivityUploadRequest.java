package io.github.abcshc.wellnessactivity.activity.api;

public record ActivityUploadRequest(
	String recordkey,
	ActivityDataRequest data,
	String type
) {
}
