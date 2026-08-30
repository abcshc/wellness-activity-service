package io.github.abcshc.wellnessactivity.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MySqlTestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberRegistrationServiceIntegrationTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	private MemberRegistrationService memberRegistrationService;

	@BeforeEach
	void setUp() {
		memberRegistrationService = new MemberRegistrationService(memberRepository, new FakePasswordHasher());
	}

	@Test
	void 해시된_비밀번호와_정규화된_이메일을_저장한다() {
		MemberRegistrationResult result = memberRegistrationService.register(new MemberRegistrationCommand(
			"홍길동",
			"길동이",
			"  GILDONG@EXAMPLE.COM  ",
			"password"
		));
		entityManager.clear();

		String passwordHash = (String) entityManager.createNativeQuery("""
			select password_hash
			from members
			where email = :email
			""")
			.setParameter("email", "gildong@example.com")
			.getSingleResult();

		assertThat(result.email()).isEqualTo("gildong@example.com");
		assertThat(passwordHash).isEqualTo("hashed-password");
	}

	private static class FakePasswordHasher implements PasswordHasher {

		@Override
		public String hash(String rawPassword) {
			return "hashed-" + rawPassword;
		}

		@Override
		public boolean matches(String rawPassword, String passwordHash) {
			return passwordHash.equals("hashed-" + rawPassword);
		}
	}
}
