package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenFamilyRevoker {

	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshTokenFamilyRevoker(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	/**
	 * 재사용 감지 뒤 호출자가 오류를 던져도 계열 폐기는 반드시 유지되어야 하므로 별도 트랜잭션으로 실행한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeActiveTokens(String familyId) {
		refreshTokenRepository.revokeActiveTokensByFamilyId(
			familyId,
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.REVOKED
		);
	}
}
