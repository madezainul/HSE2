package ahqpck.hse.safety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SafetyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafetyApplication.class, args);
	}

}
