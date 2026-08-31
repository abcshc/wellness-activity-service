package io.github.abcshc.wellnessactivity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.error.AuthErrorCode;
import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class RefreshTokenServiceIntegrationTest {

	@Autowired
	private LoginService loginService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private RefreshTokenHasher refreshTokenHasher;

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@Transactional
	void Refresh_Token을_회전하면_기존_토큰은_보존되고_새_토큰만_활성화된다() {
		MemberEntity member = memberRepository.save(new MemberEntity(
			"홍길동", "길동이", "refresh@example.com", passwordHasher.hash("password")
		));
		LoginResult loginResult = loginService.login(new LoginCommand("refresh@example.com", "password"));

		RefreshTokenResult refreshResult = refreshTokenService.refresh(new RefreshTokenCommand(loginResult.refreshToken()));

		RefreshTokenEntity previousToken = refreshTokenRepository.findByTokenHashForUpdate(
			refreshTokenHasher.hash(loginResult.refreshToken())
		).orElseThrow();
		assertThat(previousToken.isRotated()).isTrue();
		assertThat(previousToken.hasReplacement()).isTrue();
		assertThat(refreshTokenRepository.findByTokenHashAndStatus(
			refreshTokenHasher.hash(refreshResult.refreshToken()), RefreshTokenStatus.ACTIVE
		)).isPresent();
		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE)).hasSize(1);
	}

	@Test
	void 회전된_Refresh_Token을_재사용하면_계열의_활성_토큰이_폐기된다() {
		MemberEntity member = memberRepository.save(new MemberEntity(
			"홍길동", "길동이", "reuse@example.com", passwordHasher.hash("password")
		));
		LoginResult loginResult = loginService.login(new LoginCommand("reuse@example.com", "password"));
		refreshTokenService.refresh(new RefreshTokenCommand(loginResult.refreshToken()));

		assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshTokenCommand(loginResult.refreshToken())))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)
			);
		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE)).isEmpty();
	}

}
