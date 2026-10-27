package ahqpck.hse.safety;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// "h2test" uses an in-memory database (src/test/resources/application-h2test.yml).
// Without a profile this test would start the app with the "dev" profile and connect to MySQL.
@SpringBootTest
@ActiveProfiles("h2test")
class SafetyApplicationTests {

	@Test
	void contextLoads() {
	}

}
