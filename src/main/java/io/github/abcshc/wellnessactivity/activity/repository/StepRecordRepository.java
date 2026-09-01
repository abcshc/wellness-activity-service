package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepRecordRepository extends JpaRepository<StepRecordEntity, Long> {

	@Query("""
		select record
		from StepRecordEntity record
		where record.memberActivityKey = :memberActivityKey
		  and (
			(record.startedAtUtc < :endExclusive and record.endedAtUtc > :startInclusive)
			or (record.startedAtUtc = record.endedAtUtc
				and record.startedAtUtc >= :startInclusive and record.startedAtUtc < :endExclusive)
		  )
		order by record.startedAtUtc asc, record.id asc
		""")
	List<StepRecordEntity> findOverlapping(
		@Param("memberActivityKey") MemberActivityKeyEntity memberActivityKey,
		@Param("startInclusive") Instant startInclusive,
		@Param("endExclusive") Instant endExclusive
	);

	@Modifying
	@Query(value = """
		insert ignore into step_records (
			member_activity_key_id, provider, started_at_utc, ended_at_utc, steps, distance_km, calories_kcal
		)
		values (
			:memberActivityKeyId, :provider, :startedAtUtc, :endedAtUtc, :steps, :distanceKm, :caloriesKcal
		)
		""", nativeQuery = true)
	int insertIgnore(
		@Param("memberActivityKeyId") Long memberActivityKeyId,
		@Param("provider") String provider,
		@Param("startedAtUtc") Instant startedAtUtc,
		@Param("endedAtUtc") Instant endedAtUtc,
		@Param("steps") BigDecimal steps,
		@Param("distanceKm") BigDecimal distanceKm,
		@Param("caloriesKcal") BigDecimal caloriesKcal
	);
}
