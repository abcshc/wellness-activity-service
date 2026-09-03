package io.github.abcshc.wellnessactivity.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(MySqlTestContainerConfiguration.class)
@SpringBootTest(classes = WellnessActivityServiceApplication.class)
class FlywayMySqlIntegrationTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void MySQL에_현재_전체_마이그레이션을_적용한다() {
		Integer historyCount = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history",
			Integer.class
		);

		assertThat(flyway.info().all())
			.extracting(migration -> migration.getVersion().getVersion())
			.containsExactly("01", "02", "03", "04", "05", "06", "07");
		assertThat(historyCount).isEqualTo(7);
	}

	@Test
	void MySQL에_활동키와_KST_활동일을_유일하게_갖는_일별_집계_테이블을_생성한다() {
		Integer tableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = database()
				  and table_name = 'daily_activity_summaries'
				""",
			Integer.class
		);

		Integer uniqueIndexColumnCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.statistics
				where table_schema = database()
				  and table_name = 'daily_activity_summaries'
				  and index_name = 'uk_daily_activity_summaries_key_date'
				""",
			Integer.class
		);

		assertThat(tableCount).isEqualTo(1);
		assertThat(uniqueIndexColumnCount).isEqualTo(2);
	}
}
