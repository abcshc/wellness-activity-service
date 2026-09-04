package io.github.abcshc.wellnessactivity.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
	classes = WellnessActivityServiceApplication.class,
	properties = "spring.flyway.enabled=false"
)
@ContextConfiguration(initializers = ExternalFlywayMigrationIntegrationTest.ExternalMigrationInitializer.class)
class ExternalFlywayMigrationIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void 외부_Flyway_컨테이너가_적용한_스키마를_애플리케이션이_검증한다() {
		Integer historyCount = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history where success = true",
			Integer.class
		);
		Integer membersTableCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.tables "
				+ "where table_schema = database() and table_name = 'members'",
			Integer.class
		);
		Integer refreshTokensTableCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.tables "
				+ "where table_schema = database() and table_name = 'refresh_tokens'",
			Integer.class
		);
		Integer memberActivityKeysTableCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.tables "
				+ "where table_schema = database() and table_name = 'member_activity_keys'",
			Integer.class
		);
		Integer stepRecordsTableCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.tables "
				+ "where table_schema = database() and table_name = 'step_records'",
			Integer.class
		);
		Integer dailyActivitySummariesTableCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.tables "
				+ "where table_schema = database() and table_name = 'daily_activity_summaries'",
			Integer.class
		);
		Integer recordKeyUniqueConstraintCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.statistics "
				+ "where table_schema = database() and table_name = 'member_activity_keys' "
				+ "and index_name = 'uk_member_activity_keys_record_key' and non_unique = 0",
			Integer.class
		);
		Integer stepRecordUniqueConstraintCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.statistics "
				+ "where table_schema = database() and table_name = 'step_records' "
				+ "and index_name = 'uk_step_records_key_provider_period' and non_unique = 0",
			Integer.class
		);
		Integer estimatedCaloriesColumnCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.columns "
				+ "where table_schema = database() and table_name = 'step_records' "
				+ "and column_name = 'estimated_calories_kcal'",
			Integer.class
		);
		Integer estimateVersionColumnCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.columns "
				+ "where table_schema = database() and table_name = 'step_records' "
				+ "and column_name = 'calories_estimate_version'",
			Integer.class
		);
		Integer dailySummaryUniqueConstraintCount = jdbcTemplate.queryForObject(
			"select count(*) from information_schema.statistics "
				+ "where table_schema = database() and table_name = 'daily_activity_summaries' "
				+ "and index_name = 'uk_daily_activity_summaries_key_date' and non_unique = 0",
			Integer.class
		);

		assertThat(historyCount).isEqualTo(8);
		assertThat(membersTableCount).isEqualTo(1);
		assertThat(refreshTokensTableCount).isEqualTo(1);
		assertThat(memberActivityKeysTableCount).isEqualTo(1);
		assertThat(stepRecordsTableCount).isEqualTo(1);
		assertThat(dailyActivitySummariesTableCount).isEqualTo(1);
		assertThat(recordKeyUniqueConstraintCount).isEqualTo(1);
		assertThat(stepRecordUniqueConstraintCount).isEqualTo(4);
		assertThat(estimatedCaloriesColumnCount).isEqualTo(1);
		assertThat(estimateVersionColumnCount).isEqualTo(1);
		assertThat(dailySummaryUniqueConstraintCount).isEqualTo(2);
		assertThat(applicationContext.getBeansOfType(Flyway.class)).isEmpty();
	}

	@AfterAll
	static void 컨테이너를_정리한다() {
		ExternalMigrationInitializer.close();
	}

	static class ExternalMigrationInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private static final Network NETWORK = Network.newNetwork();
		private static final MySQLContainer MYSQL = new MySQLContainer(
			DockerImageName.parse("mysql:8.4.8")
		)
			.withDatabaseName("wellness_activity")
			.withUsername("test")
			.withPassword("test")
			.withNetwork(NETWORK)
			.withNetworkAliases("mysql");
		private static final GenericContainer<?> MIGRATION = new GenericContainer<>(
			DockerImageName.parse("flyway/flyway:12.4.0")
		)
			.withNetwork(NETWORK)
			.withClasspathResourceMapping("db/migration", "/flyway/sql", BindMode.READ_ONLY)
			.withEnv("FLYWAY_URL", "jdbc:mysql://mysql:3306/wellness_activity")
			.withEnv("FLYWAY_USER", "test")
			.withEnv("FLYWAY_PASSWORD", "test")
			.withEnv("FLYWAY_LOCATIONS", "filesystem:/flyway/sql")
			.withCommand("migrate")
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy())
			.withStartupTimeout(Duration.ofSeconds(60));

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			// Spring 컨텍스트가 DataSource와 JPA 검증을 초기화하기 전에 외부 Migration을 완료한다.
			MYSQL.start();
			MIGRATION.start();

			TestPropertyValues.of(
				"spring.datasource.url=" + MYSQL.getJdbcUrl(),
				"spring.datasource.username=" + MYSQL.getUsername(),
				"spring.datasource.password=" + MYSQL.getPassword()
			).applyTo(applicationContext.getEnvironment());
		}

		static void close() {
			MIGRATION.close();
			MYSQL.close();
			NETWORK.close();
		}
	}
}
