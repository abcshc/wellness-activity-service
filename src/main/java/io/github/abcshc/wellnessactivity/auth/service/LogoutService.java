package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final RefreshTokenHasher refreshTokenHasher;

	public LogoutService(
		RefreshTokenRepository refreshTokenRepository,
		RefreshTokenHasher refreshTokenHasher
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshTokenHasher = refreshTokenHasher;
	}

	@Transactional
	public void logout(LogoutCommand command) {
		refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.hash(command.refreshToken()))
			.ifPresent(refreshToken -> refreshTokenRepository.revokeActiveTokensByFamilyId(
				refreshToken.getFamilyId(),
				RefreshTokenStatus.ACTIVE,
				RefreshTokenStatus.REVOKED
			));
	}
}
