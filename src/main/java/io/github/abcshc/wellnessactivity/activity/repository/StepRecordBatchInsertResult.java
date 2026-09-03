package io.github.abcshc.wellnessactivity.activity.repository;

import java.util.List;

public record StepRecordBatchInsertResult(
	List<StepRecordBatchInsert> insertedRecords,
	int ignoredDuplicateCount
) {

	public int createdCount() {
		return insertedRecords.size();
	}
}
