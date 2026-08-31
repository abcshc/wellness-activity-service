package io.github.abcshc.wellnessactivity.activity.service;

import java.util.List;

public record ActivityUploadResult(
	int totalCount,
	int createdCount,
	int ignoredCount,
	int invalidCount,
	List<ActivityEntryValidationError> invalidEntries
) {
}
