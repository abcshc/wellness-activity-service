package io.github.abcshc.wellnessactivity.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
class FlywayConfigurationIntegrationTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void Flyway가_회원_기준선_마이그레이션을_적용한다() {
		Integer historyCount = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history",
			Integer.class
		);

		assertThat(flyway.info().all()).singleElement()
			.satisfies(migration -> {
				assertThat(migration.getVersion().getVersion()).isEqualTo("01");
				assertThat(migration.getDescription()).isEqualTo("create members");
			});
		assertThat(historyCount).isPositive();
	}
}
