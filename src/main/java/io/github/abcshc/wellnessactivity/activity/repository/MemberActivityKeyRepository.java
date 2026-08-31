package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberActivityKeyRepository extends JpaRepository<MemberActivityKeyEntity, Long> {

	List<MemberActivityKeyEntity> findAllByRecordKey(String recordKey);
}
