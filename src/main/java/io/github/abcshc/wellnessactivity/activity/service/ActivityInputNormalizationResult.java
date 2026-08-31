package io.github.abcshc.wellnessactivity.activity.service;

import java.util.List;

public record ActivityInputNormalizationResult(
	ActivityUploadCommand command,
	List<ActivityEntryValidationError> invalidEntries
) {
}
