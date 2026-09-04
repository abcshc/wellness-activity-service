package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.error.AuthErrorCode;
import io.github.abcshc.wellnessactivity.auth.token.AccessToken;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenIssuer jwtTokenIssuer;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshTokenFamilyRevoker refreshTokenFamilyRevoker;

	public RefreshTokenService(
		RefreshTokenRepository refreshTokenRepository,
		JwtTokenIssuer jwtTokenIssuer,
		RefreshTokenGenerator refreshTokenGenerator,
		RefreshTokenHasher refreshTokenHasher,
		RefreshTokenFamilyRevoker refreshTokenFamilyRevoker
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtTokenIssuer = jwtTokenIssuer;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshTokenFamilyRevoker = refreshTokenFamilyRevoker;
	}

	@Transactional
	public RefreshTokenResult refresh(RefreshTokenCommand command) {
		RefreshTokenEntity currentToken = refreshTokenRepository
			.findByTokenHashForUpdate(refreshTokenHasher.hash(command.refreshToken()))
			.orElseThrow(this::invalidRefreshToken);

		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		if (!currentToken.isActive()) {
			revokeTokenFamily(currentToken);
			throw invalidRefreshToken();
		}
		if (currentToken.isExpiredAt(now)) {
			throw invalidRefreshToken();
		}

		String nextRefreshToken = refreshTokenGenerator.generate();
		RefreshTokenEntity replacementToken = new RefreshTokenEntity(
			currentToken.getMember(),
			refreshTokenHasher.hash(nextRefreshToken),
			currentToken.getFamilyId(),
			now,
			now.plus(REFRESH_TOKEN_TTL)
		);
		refreshTokenRepository.save(replacementToken);
		if (refreshTokenRepository.rotateActiveToken(
			currentToken.getId(),
			replacementToken,
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.ROTATED
		) == 0) {
			revokeTokenFamily(currentToken);
			throw invalidRefreshToken();
		}
		AccessToken accessToken = jwtTokenIssuer.issue(currentToken.getMember().getId());
		return new RefreshTokenResult(accessToken, nextRefreshToken);
	}

	private void revokeTokenFamily(RefreshTokenEntity refreshToken) {
		refreshTokenFamilyRevoker.revokeActiveTokens(refreshToken.getFamilyId());
	}

	private BusinessException invalidRefreshToken() {
		return new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
	}
}
