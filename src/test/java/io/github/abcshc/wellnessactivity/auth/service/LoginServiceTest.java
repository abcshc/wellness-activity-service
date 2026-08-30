package io.github.abcshc.wellnessactivity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LoginServiceTest {

	private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
	private final PasswordHasher passwordHasher = Mockito.mock(PasswordHasher.class);
	private final JwtTokenIssuer jwtTokenIssuer = Mockito.mock(JwtTokenIssuer.class);
	private final RefreshTokenGenerator refreshTokenGenerator = Mockito.mock(RefreshTokenGenerator.class);
	private final RefreshTokenHasher refreshTokenHasher = Mockito.mock(RefreshTokenHasher.class);
	private final LoginService loginService = new LoginService(
		memberRepository,
		refreshTokenRepository,
		passwordHasher,
		jwtTokenIssuer,
		refreshTokenGenerator,
		refreshTokenHasher
	);

	@Test
	void 로그인하면_기존_활성_계열을_폐기하고_새_토큰_쌍을_발급한다() {
		MemberEntity member = member();
		RefreshTokenEntity existingToken = refreshToken(member, UUID.randomUUID().toString());
		AccessToken accessToken = new AccessToken("access-token", Instant.now(), Instant.now().plusSeconds(900));
		when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
		when(passwordHasher.matches("password", member.getPasswordHash())).thenReturn(true);
		when(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE))
			.thenReturn(List.of(existingToken));
		when(refreshTokenGenerator.generate()).thenReturn("raw-refresh-token");
		when(refreshTokenHasher.hash("raw-refresh-token")).thenReturn("a".repeat(64));
		when(jwtTokenIssuer.issue(member.getId())).thenReturn(accessToken);

		LoginResult result = loginService.login(new LoginCommand("member@example.com", "password"));

		ArgumentCaptor<RefreshTokenEntity> savedToken = ArgumentCaptor.forClass(RefreshTokenEntity.class);
		verify(refreshTokenRepository).revokeActiveTokensByFamilyId(
			existingToken.getFamilyId(),
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.REVOKED
		);
		verify(refreshTokenRepository).save(savedToken.capture());
		assertThat(result.accessToken()).isEqualTo(accessToken);
		assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
		assertThat(savedToken.getValue().getTokenHash()).isEqualTo("a".repeat(64));
		assertThat(savedToken.getValue().getTokenHash()).isNotEqualTo(result.refreshToken());
		assertThat(savedToken.getValue().getExpiresAt())
			.isEqualTo(savedToken.getValue().getIssuedAt().plus(14, ChronoUnit.DAYS));
	}

	@Test
	void 존재하지_않는_이메일과_비밀번호_불일치는_동일한_오류로_처리한다() {
		when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

		assertInvalidCredentials(() -> loginService.login(new LoginCommand("unknown@example.com", "password")));

		MemberEntity member = member();
		when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
		when(passwordHasher.matches("wrong-password", member.getPasswordHash())).thenReturn(false);

		assertInvalidCredentials(() -> loginService.login(new LoginCommand("member@example.com", "wrong-password")));
	}

	private void assertInvalidCredentials(ThrowingRunnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
			);
	}

	private MemberEntity member() {
		return new MemberEntity("홍길동", "길동이", "member@example.com", "password-hash");
	}

	private RefreshTokenEntity refreshToken(MemberEntity member, String familyId) {
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		return new RefreshTokenEntity(member, "b".repeat(64), familyId, issuedAt, issuedAt.plus(14, ChronoUnit.DAYS));
	}

	@FunctionalInterface
	private interface ThrowingRunnable {

		void run();
	}
}
