package io.github.abcshc.wellnessactivity.auth.token.repository;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHashAndStatus(String tokenHash, RefreshTokenStatus status);

	/**
	 * 같은 Refresh Token의 동시 갱신을 직렬화하기 위해 토큰 행을 배타적으로 잠근다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select refreshToken from RefreshTokenEntity refreshToken where refreshToken.tokenHash = :tokenHash")
	Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<RefreshTokenEntity> findAllByMemberAndStatus(MemberEntity member, RefreshTokenStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
		update RefreshTokenEntity refreshToken
		set refreshToken.status = :rotatedStatus,
			refreshToken.replacedByToken = :replacementToken
		where refreshToken.id = :tokenId
		and refreshToken.status = :activeStatus
		""")
	int rotateActiveToken(
		@Param("tokenId") Long tokenId,
		@Param("replacementToken") RefreshTokenEntity replacementToken,
		@Param("activeStatus") RefreshTokenStatus activeStatus,
		@Param("rotatedStatus") RefreshTokenStatus rotatedStatus
	);

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
