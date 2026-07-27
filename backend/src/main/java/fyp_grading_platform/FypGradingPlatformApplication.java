package fyp_grading_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FypGradingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(FypGradingPlatformApplication.class, args);
	}

}
