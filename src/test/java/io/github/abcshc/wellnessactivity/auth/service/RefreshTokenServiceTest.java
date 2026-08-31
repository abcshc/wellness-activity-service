package io.github.abcshc.wellnessactivity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.auth.error.AuthErrorCode;
import io.github.abcshc.wellnessactivity.auth.token.AccessToken;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RefreshTokenServiceTest {

	private final RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
	private final JwtTokenIssuer jwtTokenIssuer = Mockito.mock(JwtTokenIssuer.class);
	private final RefreshTokenGenerator refreshTokenGenerator = Mockito.mock(RefreshTokenGenerator.class);
	private final RefreshTokenHasher refreshTokenHasher = Mockito.mock(RefreshTokenHasher.class);
	private final RefreshTokenFamilyRevoker refreshTokenFamilyRevoker = Mockito.mock(RefreshTokenFamilyRevoker.class);
	private final RefreshTokenService refreshTokenService = new RefreshTokenService(
		refreshTokenRepository,
		jwtTokenIssuer,
		refreshTokenGenerator,
		refreshTokenHasher,
		refreshTokenFamilyRevoker
	);

	@Test
	void 유효한_Refresh_Token은_같은_계열의_새_토큰_쌍으로_회전한다() {
		MemberEntity member = member();
		RefreshTokenEntity currentToken = activeToken(member);
		AccessToken accessToken = new AccessToken("new-access-token", Instant.now(), Instant.now().plusSeconds(900));
		when(refreshTokenHasher.hash("presented-token")).thenReturn("a".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("a".repeat(64))).thenReturn(Optional.of(currentToken));
		when(refreshTokenGenerator.generate()).thenReturn("next-token");
		when(refreshTokenHasher.hash("next-token")).thenReturn("b".repeat(64));
		when(refreshTokenRepository.rotateActiveToken(
			any(), any(), any(), any()
		)).thenReturn(1);
		when(jwtTokenIssuer.issue(member.getId())).thenReturn(accessToken);

		RefreshTokenResult result = refreshTokenService.refresh(new RefreshTokenCommand("presented-token"));

		ArgumentCaptor<RefreshTokenEntity> replacement = ArgumentCaptor.forClass(RefreshTokenEntity.class);
		verify(refreshTokenRepository).save(replacement.capture());
		assertThat(result.accessToken()).isEqualTo(accessToken);
		assertThat(result.refreshToken()).isEqualTo("next-token");
		assertThat(replacement.getValue().isActive()).isTrue();
		assertThat(replacement.getValue().getFamilyId()).isEqualTo(currentToken.getFamilyId());
		assertThat(replacement.getValue().getExpiresAt())
			.isEqualTo(replacement.getValue().getIssuedAt().plus(14, ChronoUnit.DAYS));
	}

	@Test
	void 존재하지_않거나_만료된_Refresh_Token은_동일한_오류로_처리한다() {
		when(refreshTokenHasher.hash("unknown-token")).thenReturn("a".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("a".repeat(64))).thenReturn(Optional.empty());

		assertInvalidRefreshToken(() -> refreshTokenService.refresh(new RefreshTokenCommand("unknown-token")));

		RefreshTokenEntity expiredToken = new RefreshTokenEntity(
			member(), "b".repeat(64), "family-id", Instant.now().minus(15, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS)
		);
		when(refreshTokenHasher.hash("expired-token")).thenReturn("b".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("b".repeat(64))).thenReturn(Optional.of(expiredToken));

		assertInvalidRefreshToken(() -> refreshTokenService.refresh(new RefreshTokenCommand("expired-token")));
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void 회전된_Refresh_Token을_재사용하면_활성_계열을_폐기한다() {
		RefreshTokenEntity rotatedToken = mock(RefreshTokenEntity.class);
		when(rotatedToken.isActive()).thenReturn(false);
		when(rotatedToken.getFamilyId()).thenReturn("family-id");
		when(refreshTokenHasher.hash("reused-token")).thenReturn("a".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("a".repeat(64))).thenReturn(Optional.of(rotatedToken));

		assertInvalidRefreshToken(() -> refreshTokenService.refresh(new RefreshTokenCommand("reused-token")));

		verify(refreshTokenFamilyRevoker).revokeActiveTokens("family-id");
	}

	private void assertInvalidRefreshToken(ThrowingRunnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)
			);
	}

	private MemberEntity member() {
		return new MemberEntity("홍길동", "길동이", "member@example.com", "password-hash");
	}

	private RefreshTokenEntity activeToken(MemberEntity member) {
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		return new RefreshTokenEntity(member, "c".repeat(64), "family-id", issuedAt, issuedAt.plus(14, ChronoUnit.DAYS));
	}

	@FunctionalInterface
	private interface ThrowingRunnable {

		void run();
	}
}
