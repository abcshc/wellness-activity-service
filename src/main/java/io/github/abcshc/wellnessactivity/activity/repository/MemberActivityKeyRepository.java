package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberActivityKeyRepository extends JpaRepository<MemberActivityKeyEntity, Long> {

	Optional<MemberActivityKeyEntity> findByRecordKey(String recordKey);

	@Modifying
	@Query(value = """
		insert ignore into member_activity_keys (member_id, record_key)
		values (:memberId, :recordKey)
		""", nativeQuery = true)
	int insertIgnore(@Param("memberId") Long memberId, @Param("recordKey") String recordKey);
}
