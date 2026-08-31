package io.github.abcshc.wellnessactivity.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MySqlTestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StepRecordRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 활동_주체와_원본_이벤트를_저장하고_recordkey로_조회할_수_있다() {
		MemberEntity member = savedMember();
		MemberActivityKeyEntity memberActivityKey = memberActivityKeyRepository.saveAndFlush(
			new MemberActivityKeyEntity(member, "record-key-001")
		);
		stepRecordRepository.saveAndFlush(stepRecord(
			memberActivityKey,
			Instant.parse("2024-11-22T00:00:00Z"),
			Instant.parse("2024-11-22T00:10:00Z")
		));
		entityManager.clear();

		assertThat(memberActivityKeyRepository.findAllByRecordKey("record-key-001")).hasSize(1);
		assertThat(stepRecordRepository.count()).isEqualTo(1);
	}

	@Test
	void 시작_시각이_같아도_종료_시각이_다르면_별도_활동으로_저장한다() {
		MemberActivityKeyEntity memberActivityKey = savedMemberActivityKey();
		Instant startedAt = Instant.parse("2024-11-22T00:00:00Z");
		stepRecordRepository.saveAndFlush(stepRecord(
			memberActivityKey,
			startedAt,
			startedAt
		));
		stepRecordRepository.saveAndFlush(stepRecord(
			memberActivityKey,
			startedAt,
			Instant.parse("2024-11-22T00:10:00Z")
		));

		assertThat(stepRecordRepository.count()).isEqualTo(2);
	}

	@Test
	void provider가_다른_같은_시간_구간은_별도_활동으로_저장한다() {
		MemberActivityKeyEntity memberActivityKey = savedMemberActivityKey();
		Instant startedAt = Instant.parse("2024-11-22T00:00:00Z");
		Instant endedAt = Instant.parse("2024-11-22T00:10:00Z");
		stepRecordRepository.saveAndFlush(stepRecord(
			memberActivityKey,
			ActivityProvider.SAMSUNG_HEALTH,
			startedAt,
			endedAt
		));
		stepRecordRepository.saveAndFlush(stepRecord(
			memberActivityKey,
			ActivityProvider.APPLE_HEALTH,
			startedAt,
			endedAt
		));

		assertThat(stepRecordRepository.count()).isEqualTo(2);
	}

	private MemberActivityKeyEntity savedMemberActivityKey() {
		return memberActivityKeyRepository.saveAndFlush(new MemberActivityKeyEntity(savedMember(), "record-key-001"));
	}

	private MemberEntity savedMember() {
		return memberRepository.saveAndFlush(new MemberEntity(
			"홍길동",
			"길동이",
			"gildong@example.com",
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
		));
	}

	private StepRecordEntity stepRecord(
		MemberActivityKeyEntity memberActivityKey,
		Instant startedAt,
		Instant endedAt
	) {
		return stepRecord(memberActivityKey, ActivityProvider.SAMSUNG_HEALTH, startedAt, endedAt);
	}

	private StepRecordEntity stepRecord(
		MemberActivityKeyEntity memberActivityKey,
		ActivityProvider provider,
		Instant startedAt,
		Instant endedAt
	) {
		return new StepRecordEntity(
			memberActivityKey,
			provider,
			startedAt,
			endedAt,
			new BigDecimal("32"),
			new BigDecimal("0.024220001"),
			new BigDecimal("1.21")
		);
	}
}
