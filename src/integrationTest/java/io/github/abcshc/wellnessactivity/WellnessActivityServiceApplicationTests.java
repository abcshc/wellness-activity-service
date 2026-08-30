package io.github.abcshc.wellnessactivity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
class WellnessActivityServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
