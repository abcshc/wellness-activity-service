package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import java.util.List;

public record ActivityUploadCommand(
	String recordKey,
	ActivityProvider provider,
	List<NormalizedStepRecordCommand> records
) {
}
