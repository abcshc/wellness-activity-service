package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryRepository;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@Import({MySqlTestContainerConfiguration.class, ActivityUploadTransactionIntegrationTest.FailingDailySummaryConfiguration.class})
class ActivityUploadTransactionIntegrationTest {

	@Autowired
	private ActivityUploadService activityUploadService;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		stepRecordRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 일별_집계_갱신에_실패하면_원본_이벤트와_활동키도_함께_롤백한다() {
		MemberEntity member = memberRepository.saveAndFlush(new MemberEntity(
			"트랜잭션", "검증", "transaction@example.com", "password-hash"
		));

		assertThatThrownBy(() -> activityUploadService.upload(member.getId(), input()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("daily summary update failed");

		assertThat(stepRecordRepository.count()).isZero();
		assertThat(memberActivityKeyRepository.findByRecordKey("record-key-transaction")).isEmpty();
		assertThat(jdbcTemplate.queryForObject("select count(*) from daily_activity_summaries", Long.class)).isZero();
	}

	private ActivityInputNormalizationResult input() {
		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(
				"record-key-transaction",
				ActivityProvider.SAMSUNG_HEALTH,
				List.of(new NormalizedStepRecordCommand(
					Instant.parse("2024-11-15T00:00:00Z"),
					Instant.parse("2024-11-15T00:10:00Z"),
					new BigDecimal("100"), new BigDecimal("0.08"), new BigDecimal("4")
				))
			),
			List.of()
		);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FailingDailySummaryConfiguration {

		@Bean
		@Primary
		DailyActivitySummaryRepository failingDailyActivitySummaryRepository() {
			DailyActivitySummaryRepository repository = mock(DailyActivitySummaryRepository.class);
			doThrow(new IllegalStateException("daily summary update failed"))
				.when(repository).upsert(anyLong(), any(), any(), any(), any(), any());
			return repository;
		}
	}
}
