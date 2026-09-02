package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import java.time.Instant;
import java.util.List;
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

}
