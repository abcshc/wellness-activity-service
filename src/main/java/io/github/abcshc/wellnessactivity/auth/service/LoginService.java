package io.github.abcshc.wellnessactivity.auth.service;

import io.github.abcshc.wellnessactivity.auth.error.AuthErrorCode;
import io.github.abcshc.wellnessactivity.auth.token.AccessToken;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

	private static final long REFRESH_TOKEN_TTL_DAYS = 14;

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordHasher passwordHasher;
	private final JwtTokenIssuer jwtTokenIssuer;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;

	public LoginService(
		MemberRepository memberRepository,
		RefreshTokenRepository refreshTokenRepository,
		PasswordHasher passwordHasher,
		JwtTokenIssuer jwtTokenIssuer,
		RefreshTokenGenerator refreshTokenGenerator,
		RefreshTokenHasher refreshTokenHasher
	) {
		this.memberRepository = memberRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordHasher = passwordHasher;
		this.jwtTokenIssuer = jwtTokenIssuer;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenHasher = refreshTokenHasher;
	}

	@Transactional
	public LoginResult login(LoginCommand command) {
		MemberEntity member = memberRepository.findByEmail(normalizeEmail(command.email()))
			.orElseThrow(this::invalidCredentials);

		if (!passwordHasher.matches(command.password(), member.getPasswordHash())) {
			throw invalidCredentials();
		}

		revokeActiveTokenFamilies(member);

		String refreshToken = refreshTokenGenerator.generate();
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity(
			member,
			refreshTokenHasher.hash(refreshToken),
			UUID.randomUUID().toString(),
			issuedAt,
			issuedAt.plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS)
		);
		refreshTokenRepository.save(refreshTokenEntity);

		AccessToken accessToken = jwtTokenIssuer.issue(member.getId());
		return new LoginResult(accessToken, refreshToken);
	}

	private void revokeActiveTokenFamilies(MemberEntity member) {
		refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE)
			.stream()
			.map(RefreshTokenEntity::getFamilyId)
			.distinct()
			.forEach(familyId -> refreshTokenRepository.revokeActiveTokensByFamilyId(
				familyId,
				RefreshTokenStatus.ACTIVE,
				RefreshTokenStatus.REVOKED
			));
	}

	private String normalizeEmail(String email) {
		return email.toLowerCase(Locale.ROOT);
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
	}
}
