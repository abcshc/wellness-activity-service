package io.github.abcshc.wellnessactivity.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MemberRegistrationServiceTest {

	@Mock
	private MemberRepository memberRepository;

	private FakePasswordHasher passwordHasher;
	private MemberRegistrationService memberRegistrationService;

	@BeforeEach
	void setUp() {
		passwordHasher = new FakePasswordHasher();
		memberRegistrationService = new MemberRegistrationService(memberRepository, passwordHasher);
	}

	@Test
	void 이메일을_정규화하고_비밀번호를_해시해_회원으로_저장한다() {
		MemberRegistrationCommand command = new MemberRegistrationCommand(
			"홍길동",
			"길동이",
			"  GILDONG@EXAMPLE.COM  ",
			"password"
		);
		when(memberRepository.existsByEmail("gildong@example.com")).thenReturn(false);

		MemberRegistrationResult result = memberRegistrationService.register(command);

		assertThat(result).isEqualTo(new MemberRegistrationResult(
			"홍길동",
			"길동이",
			"gildong@example.com"
		));
		assertThat(passwordHasher.lastRawPassword).isEqualTo("password");
		verify(memberRepository).existsByEmail("gildong@example.com");
		verify(memberRepository).saveAndFlush(any());
	}

	@Test
	void 이미_가입한_이메일이면_해시와_저장_없이_중복_오류를_반환한다() {
		MemberRegistrationCommand command = new MemberRegistrationCommand(
			"홍길동",
			"길동이",
			"gildong@example.com",
			"password"
		);
		when(memberRepository.existsByEmail("gildong@example.com")).thenReturn(true);

		assertThatThrownBy(() -> memberRegistrationService.register(command))
			.isInstanceOf(DuplicateEmailException.class);

		assertThat(passwordHasher.lastRawPassword).isNull();
		verify(memberRepository, never()).saveAndFlush(any());
	}

	@Test
	void 저장_중_발생한_이메일_유니크_제약조건_오류를_중복_오류로_변환한다() {
		MemberRegistrationCommand command = new MemberRegistrationCommand(
			"홍길동",
			"길동이",
			"gildong@example.com",
			"password"
		);
		when(memberRepository.existsByEmail("gildong@example.com")).thenReturn(false);
		when(memberRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate email"));

		assertThatThrownBy(() -> memberRegistrationService.register(command))
			.isInstanceOf(DuplicateEmailException.class);

		assertThat(passwordHasher.lastRawPassword).isEqualTo("password");
	}

	private static class FakePasswordHasher implements PasswordHasher {

		private String lastRawPassword;

		@Override
		public String hash(String rawPassword) {
			lastRawPassword = rawPassword;
			return "hashed-" + rawPassword;
		}

		@Override
		public boolean matches(String rawPassword, String passwordHash) {
			return passwordHash.equals("hashed-" + rawPassword);
		}
	}
}
