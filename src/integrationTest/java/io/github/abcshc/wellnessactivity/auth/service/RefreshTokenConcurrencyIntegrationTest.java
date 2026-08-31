package io.github.abcshc.wellnessactivity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.member.service.PasswordHasher;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class RefreshTokenConcurrencyIntegrationTest {

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

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 동일한_Refresh_Token의_동시_갱신은_하나만_성공하고_후속_재사용은_계열을_폐기한다() throws Exception {
		MemberEntity member = memberRepository.saveAndFlush(new MemberEntity(
			"홍길동", "길동이", "concurrent-refresh@example.com", passwordHasher.hash("password")
		));
		LoginResult loginResult = loginService.login(new LoginCommand("concurrent-refresh@example.com", "password"));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		try {
			List<Future<Boolean>> results = List.of(
				executorService.submit(refreshTask(loginResult.refreshToken(), ready, start)),
				executorService.submit(refreshTask(loginResult.refreshToken(), ready, start))
			);
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			long successfulRefreshes = 0;
			for (Future<Boolean> result : results) {
				if (result.get(10, TimeUnit.SECONDS)) {
					successfulRefreshes++;
				}
			}

			assertThat(successfulRefreshes).isEqualTo(1);
		} finally {
			executorService.shutdownNow();
		}

		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE))
			.isEmpty();
	}

	private Callable<Boolean> refreshTask(String refreshToken, CountDownLatch ready, CountDownLatch start) {
		return () -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			try {
				refreshTokenService.refresh(new RefreshTokenCommand(refreshToken));
				return true;
			} catch (BusinessException exception) {
				return false;
			}
		};
	}
}
