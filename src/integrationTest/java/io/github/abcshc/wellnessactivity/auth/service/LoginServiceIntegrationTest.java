package io.github.abcshc.wellnessactivity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
class LoginServiceIntegrationTest {

	@Autowired
	private LoginService loginService;

	@Autowired
	private RefreshTokenHasher refreshTokenHasher;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Test
	void 새_로그인은_기존_활성_Refresh_Token을_폐기하고_해시만_저장한다() {
		MemberEntity member = memberRepository.saveAndFlush(new MemberEntity(
			"홍길동",
			"길동이",
			"login@example.com",
			passwordHasher.hash("password")
		));

		LoginResult firstLogin = loginService.login(new LoginCommand("LOGIN@EXAMPLE.COM", "password"));
		LoginResult secondLogin = loginService.login(new LoginCommand("login@example.com", "password"));

		assertThat(refreshTokenRepository.findByTokenHashAndStatus(
			refreshTokenHasher.hash(firstLogin.refreshToken()),
			RefreshTokenStatus.ACTIVE
		)).isEmpty();
		assertThat(refreshTokenRepository.findByTokenHashAndStatus(
			refreshTokenHasher.hash(secondLogin.refreshToken()),
			RefreshTokenStatus.ACTIVE
		)).isPresent();
		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE))
			.hasSize(1);
	}
}
