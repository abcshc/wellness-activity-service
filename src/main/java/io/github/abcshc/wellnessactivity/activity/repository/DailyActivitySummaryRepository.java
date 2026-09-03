package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.DailyActivitySummaryEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyActivitySummaryRepository extends JpaRepository<DailyActivitySummaryEntity, Long> {

	@Modifying
	@Query(value = """
		insert into daily_activity_summaries (
			member_activity_key_id, activity_date, steps, distance_km, source_calories_kcal, estimated_calories_kcal
		)
		values (
			:memberActivityKeyId, :activityDate, :steps, :distanceKm, :sourceCaloriesKcal, :estimatedCaloriesKcal
		) as incoming
		on duplicate key update
			steps = daily_activity_summaries.steps + incoming.steps,
			distance_km = daily_activity_summaries.distance_km + incoming.distance_km,
			source_calories_kcal = daily_activity_summaries.source_calories_kcal + incoming.source_calories_kcal,
			estimated_calories_kcal = daily_activity_summaries.estimated_calories_kcal + incoming.estimated_calories_kcal
		""", nativeQuery = true)
	int upsert(
		@Param("memberActivityKeyId") Long memberActivityKeyId,
		@Param("activityDate") LocalDate activityDate,
		@Param("steps") BigDecimal steps,
		@Param("distanceKm") BigDecimal distanceKm,
		@Param("sourceCaloriesKcal") BigDecimal sourceCaloriesKcal,
		@Param("estimatedCaloriesKcal") BigDecimal estimatedCaloriesKcal
	);
}
