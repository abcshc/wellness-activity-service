package io.github.abcshc.wellnessactivity.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import(MySqlTestContainerConfiguration.class)
class MemberRepositoryMySqlIntegrationTest {

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	void 실제_MySQL은_이메일_유니크_제약조건명을_예외_원인에_포함한다() {
		memberRepository.saveAndFlush(member("gildong@example.com"));

		Throwable exception = catchThrowable(() -> memberRepository.saveAndFlush(member("gildong@example.com")));

		assertThat(exception).isInstanceOfSatisfying(DataIntegrityViolationException.class, dataIntegrityViolation ->
			assertThat(findConstraintViolation(dataIntegrityViolation).getConstraintName()).endsWith(".uk_members_email")
		);
	}

	private MemberEntity member(String email) {
		return new MemberEntity(
			"홍길동",
			"길동이",
			email,
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		);
	}

	private ConstraintViolationException findConstraintViolation(Throwable exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				return constraintViolation;
			}
			cause = cause.getCause();
		}

		throw new AssertionError("Hibernate ConstraintViolationException을 찾을 수 없습니다.");
	}
}
