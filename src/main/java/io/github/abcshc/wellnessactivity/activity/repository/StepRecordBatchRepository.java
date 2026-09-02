package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StepRecordBatchRepository {

	private static final String ROW_PLACEHOLDERS = "(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final JdbcTemplate jdbcTemplate;

	public StepRecordBatchRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int insertIgnore(
		Long memberActivityKeyId,
		ActivityProvider provider,
		List<StepRecordBatchInsert> records
	) {
		if (records.isEmpty()) {
			return 0;
		}
		return jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(insertSql(records.size()));
			int parameterIndex = 1;
			for (StepRecordBatchInsert record : records) {
				statement.setLong(parameterIndex++, memberActivityKeyId);
				statement.setString(parameterIndex++, provider.name());
				// DATETIME은 시간대를 저장하지 않으므로, UTC LocalDateTime으로 명시해 저장한다.
				statement.setObject(parameterIndex++, LocalDateTime.ofInstant(record.startedAtUtc(), ZoneOffset.UTC));
				statement.setObject(parameterIndex++, LocalDateTime.ofInstant(record.endedAtUtc(), ZoneOffset.UTC));
				statement.setBigDecimal(parameterIndex++, record.steps());
				statement.setBigDecimal(parameterIndex++, record.distanceKm());
				statement.setBigDecimal(parameterIndex++, record.caloriesKcal());
				statement.setBigDecimal(parameterIndex++, record.estimatedCaloriesKcal());
				statement.setString(parameterIndex++, record.caloriesEstimateVersion());
			}
			return statement;
		});
	}

	private String insertSql(int recordCount) {
		StringJoiner values = new StringJoiner(", ");
		for (int index = 0; index < recordCount; index++) {
			values.add(ROW_PLACEHOLDERS);
		}
		return """
			insert ignore into step_records (
				member_activity_key_id, provider, started_at_utc, ended_at_utc, steps, distance_km, calories_kcal,
				estimated_calories_kcal, calories_estimate_version
			)
			values %s
			""".formatted(values);
	}
}
