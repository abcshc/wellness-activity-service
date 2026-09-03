package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StepRecordBatchRepository {

	private final JdbcTemplate jdbcTemplate;

	public StepRecordBatchRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public StepRecordBatchInsertResult insertIgnore(
		Long memberActivityKeyId,
		ActivityProvider provider,
		List<StepRecordBatchInsert> records
	) {
		if (records.isEmpty()) {
			return new StepRecordBatchInsertResult(List.of(), 0);
		}
		return jdbcTemplate.execute((ConnectionCallback<StepRecordBatchInsertResult>) connection -> {
			try (PreparedStatement statement = connection.prepareStatement(insertSql())) {
				for (StepRecordBatchInsert record : records) {
					bind(statement, memberActivityKeyId, provider, record);
					statement.addBatch();
				}
				return toInsertResult(records, statement.executeBatch());
			}
		});
	}

	private void bind(
		PreparedStatement statement,
		Long memberActivityKeyId,
		ActivityProvider provider,
		StepRecordBatchInsert record
	) throws java.sql.SQLException {
		int parameterIndex = 1;
		statement.setLong(parameterIndex++, memberActivityKeyId);
		statement.setString(parameterIndex++, provider.name());
		// DATETIME은 시간대를 저장하지 않으므로, UTC LocalDateTime으로 명시해 저장한다.
		statement.setObject(parameterIndex++, LocalDateTime.ofInstant(record.startedAtUtc(), ZoneOffset.UTC));
		statement.setObject(parameterIndex++, LocalDateTime.ofInstant(record.endedAtUtc(), ZoneOffset.UTC));
		statement.setBigDecimal(parameterIndex++, record.steps());
		statement.setBigDecimal(parameterIndex++, record.distanceKm());
		statement.setBigDecimal(parameterIndex++, record.caloriesKcal());
		statement.setBigDecimal(parameterIndex++, record.estimatedCaloriesKcal());
		statement.setString(parameterIndex, record.caloriesEstimateVersion());
	}

	private StepRecordBatchInsertResult toInsertResult(List<StepRecordBatchInsert> records, int[] updateCounts) {
		if (updateCounts.length != records.size()) {
			throw new IllegalStateException("원본 활동 배치 저장 결과의 항목 수가 입력과 다릅니다.");
		}

		List<StepRecordBatchInsert> insertedRecords = new ArrayList<>();
		int ignoredDuplicateCount = 0;
		for (int index = 0; index < updateCounts.length; index++) {
			int updateCount = updateCounts[index];
			if (updateCount == 1) {
				insertedRecords.add(records.get(index));
				continue;
			}
			if (updateCount == 0) {
				ignoredDuplicateCount++;
				continue;
			}
			// 항목별 결과를 잃으면 일별 집계에 신규·중복 데이터를 정확히 반영할 수 없다.
			if (updateCount == Statement.SUCCESS_NO_INFO) {
				throw new IllegalStateException("원본 활동 배치 저장의 항목별 결과를 확인할 수 없습니다.");
			}
			throw new IllegalStateException("원본 활동 배치 저장 결과가 올바르지 않습니다: " + updateCount);
		}
		return new StepRecordBatchInsertResult(insertedRecords, ignoredDuplicateCount);
	}

	private String insertSql() {
		return """
			insert ignore into step_records (
				member_activity_key_id, provider, started_at_utc, ended_at_utc, steps, distance_km, calories_kcal,
				estimated_calories_kcal, calories_estimate_version
			)
			values (?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";
	}
}
