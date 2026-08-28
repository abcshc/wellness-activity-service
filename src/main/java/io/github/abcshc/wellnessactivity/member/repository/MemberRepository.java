package io.github.abcshc.wellnessactivity.member.repository;

import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	Optional<MemberEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
