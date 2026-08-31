package io.github.abcshc.wellnessactivity.auth.token.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.auth.token.entity.RefreshTokenEntity;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import(MySqlTestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 토큰_해시로_활성_Refresh_Token을_조회할_수_있다() {
		MemberEntity member = savedMember("find@example.com");
		String tokenHash = "a".repeat(64);
		refreshTokenRepository.saveAndFlush(refreshToken(member, tokenHash, UUID.randomUUID().toString()));
		entityManager.clear();

		boolean found = refreshTokenRepository
			.findByTokenHashAndStatus(tokenHash, RefreshTokenStatus.ACTIVE)
			.isPresent();

		assertThat(found).isTrue();
	}

	@Test
	void 같은_토큰_해시는_데이터베이스_유니크_제약조건으로_저장할_수_없다() {
		MemberEntity member = savedMember("unique@example.com");
		String tokenHash = "b".repeat(64);
		refreshTokenRepository.saveAndFlush(refreshToken(member, tokenHash, UUID.randomUUID().toString()));

		assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
			refreshToken(member, tokenHash, UUID.randomUUID().toString())
		))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void Refresh_Token_조회와_정리를_위한_인덱스가_있다() {
		List<String> indexNames = jdbcTemplate.queryForList("""
			select distinct index_name
			from information_schema.statistics
			where table_schema = database()
			and table_name = 'refresh_tokens'
			""", String.class);

		assertThat(indexNames).contains(
			"uk_refresh_tokens_token_hash",
			"idx_refresh_tokens_member_status",
			"idx_refresh_tokens_family_status",
			"idx_refresh_tokens_status_expires_at"
		);
	}

	@Test
	void 회원의_활성_토큰을_조회하고_토큰_계열별로_폐기할_수_있다() {
		MemberEntity member = savedMember("revoke@example.com");
		String revokedFamilyId = UUID.randomUUID().toString();
		refreshTokenRepository.saveAndFlush(refreshToken(member, "c".repeat(64), revokedFamilyId));
		refreshTokenRepository.saveAndFlush(refreshToken(member, "d".repeat(64), UUID.randomUUID().toString()));

		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE)).hasSize(2);
		assertThat(refreshTokenRepository.revokeActiveTokensByFamilyId(
			revokedFamilyId,
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.REVOKED
		)).isEqualTo(1);
		entityManager.clear();

		assertThat(refreshTokenRepository.findAllByMemberAndStatus(member, RefreshTokenStatus.ACTIVE)).hasSize(1);
	}

	@Test
	void 활성_Refresh_Token은_조건부_갱신으로_한번만_회전할_수_있다() {
		MemberEntity member = savedMember("rotate@example.com");
		String familyId = UUID.randomUUID().toString();
		RefreshTokenEntity currentToken = refreshToken(member, "e".repeat(64), familyId);
		RefreshTokenEntity replacementToken = refreshToken(member, "f".repeat(64), familyId);
		refreshTokenRepository.saveAndFlush(currentToken);
		refreshTokenRepository.saveAndFlush(replacementToken);

		assertThat(refreshTokenRepository.rotateActiveToken(
			currentToken.getId(),
			replacementToken,
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.ROTATED
		)).isEqualTo(1);
		assertThat(refreshTokenRepository.rotateActiveToken(
			currentToken.getId(),
			replacementToken,
			RefreshTokenStatus.ACTIVE,
			RefreshTokenStatus.ROTATED
		)).isZero();
		entityManager.clear();

		RefreshTokenEntity rotatedToken = refreshTokenRepository.findByTokenHashForUpdate("e".repeat(64)).orElseThrow();
		assertThat(rotatedToken.isRotated()).isTrue();
		assertThat(rotatedToken.hasReplacement()).isTrue();
	}

	private MemberEntity savedMember(String email) {
		return memberRepository.saveAndFlush(new MemberEntity(
			"홍길동",
			"길동이",
			email,
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		));
	}

	private RefreshTokenEntity refreshToken(MemberEntity member, String tokenHash, String familyId) {
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		return new RefreshTokenEntity(member, tokenHash, familyId, issuedAt, issuedAt.plus(14, ChronoUnit.DAYS));
	}
}
