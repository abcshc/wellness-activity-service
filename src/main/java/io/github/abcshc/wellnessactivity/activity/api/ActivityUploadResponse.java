package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.ActivityEntryValidationError;
import io.github.abcshc.wellnessactivity.activity.service.ActivityUploadResult;
import java.util.List;

public record ActivityUploadResponse(
	int totalCount,
	int createdCount,
	int ignoredCount,
	int invalidCount,
	List<ActivityUploadInvalidEntryResponse> invalidEntries
) {

	public static ActivityUploadResponse from(ActivityUploadResult result) {
		return new ActivityUploadResponse(
			result.totalCount(),
			result.createdCount(),
			result.ignoredCount(),
			result.invalidCount(),
			result.invalidEntries().stream().map(ActivityUploadInvalidEntryResponse::from).toList()
		);
	}

	public record ActivityUploadInvalidEntryResponse(
		int index,
		String field,
		String code,
		String message
	) {

		private static ActivityUploadInvalidEntryResponse from(ActivityEntryValidationError error) {
			return new ActivityUploadInvalidEntryResponse(error.index(), error.field(), error.code(), error.message());
		}
	}
}
