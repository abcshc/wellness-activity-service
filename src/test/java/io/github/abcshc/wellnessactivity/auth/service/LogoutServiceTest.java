package io.github.abcshc.wellnessactivity.auth.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LogoutServiceTest {

	private final RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
	private final RefreshTokenHasher refreshTokenHasher = Mockito.mock(RefreshTokenHasher.class);
	private final LogoutService logoutService = new LogoutService(refreshTokenRepository, refreshTokenHasher);

	@Test
	void Refresh_Token이_존재하면_같은_계열의_활성_토큰을_폐기한다() {
		RefreshTokenEntity refreshToken = Mockito.mock(RefreshTokenEntity.class);
		when(refreshTokenHasher.hash("presented-token")).thenReturn("a".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("a".repeat(64))).thenReturn(Optional.of(refreshToken));
		when(refreshToken.getFamilyId()).thenReturn("family-id");

		logoutService.logout(new LogoutCommand("presented-token"));

		verify(refreshTokenRepository).revokeActiveTokensByFamilyId(
			"family-id", RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED
		);
	}

	@Test
	void 존재하지_않는_Refresh_Token도_멱등하게_처리한다() {
		when(refreshTokenHasher.hash("unknown-token")).thenReturn("a".repeat(64));
		when(refreshTokenRepository.findByTokenHashForUpdate("a".repeat(64))).thenReturn(Optional.empty());

		logoutService.logout(new LogoutCommand("unknown-token"));

		verify(refreshTokenRepository, never()).revokeActiveTokensByFamilyId(
			Mockito.anyString(), Mockito.any(), Mockito.any()
		);
	}
}
