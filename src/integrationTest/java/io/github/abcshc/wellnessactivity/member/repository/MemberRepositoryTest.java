package io.github.abcshc.wellnessactivity.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(MySqlTestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 이메일로_저장한_회원을_조회할_수_있다() {
		MemberEntity member = new MemberEntity(
			"홍길동",
			"길동이",
			"gildong@example.com",
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		);

		memberRepository.saveAndFlush(member);
		entityManager.clear();

		boolean found = memberRepository.findByEmail("gildong@example.com").isPresent();

		assertThat(found).isTrue();
		assertThat(memberRepository.existsByEmail("gildong@example.com")).isTrue();
		assertThat(memberRepository.existsByEmail("unknown@example.com")).isFalse();
	}

	@Test
	void 같은_이메일은_데이터베이스_유니크_제약조건으로_저장할_수_없다() {
		memberRepository.saveAndFlush(new MemberEntity(
			"홍길동",
			"길동이",
			"gildong@example.com",
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		));

		assertThatThrownBy(() -> memberRepository.saveAndFlush(new MemberEntity(
			"임꺽정",
			"꺽정이",
			"gildong@example.com",
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
