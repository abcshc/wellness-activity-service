package io.github.abcshc.wellnessactivity.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
class PasswordHasherIntegrationTest {

	@Autowired
	private MemberRegistrationService memberRegistrationService;

	@Autowired
	private PasswordHasher passwordHasher;

	@Test
	void 회원가입_서비스에_BCrypt_비밀번호_해시_구현체가_연결된다() {
		assertThat(memberRegistrationService).isNotNull();
		assertThat(passwordHasher).isInstanceOf(BCryptPasswordHasher.class);
	}
}
