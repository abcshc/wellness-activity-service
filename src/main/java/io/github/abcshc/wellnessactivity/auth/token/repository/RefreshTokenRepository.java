package io.github.abcshc.wellnessactivity.auth.token.repository;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHashAndStatus(String tokenHash, RefreshTokenStatus status);

	List<RefreshTokenEntity> findAllByMemberAndStatus(MemberEntity member, RefreshTokenStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
		update RefreshTokenEntity refreshToken
		set refreshToken.status = :revokedStatus
		where refreshToken.familyId = :familyId
		and refreshToken.status = :activeStatus
		""")
	int revokeActiveTokensByFamilyId(
		@Param("familyId") String familyId,
		@Param("activeStatus") RefreshTokenStatus activeStatus,
		@Param("revokedStatus") RefreshTokenStatus revokedStatus
	);
}
