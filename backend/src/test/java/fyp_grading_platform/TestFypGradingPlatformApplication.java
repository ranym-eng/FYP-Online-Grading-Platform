package fyp_grading_platform;

import org.springframework.boot.SpringApplication;

public class TestFypGradingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(FypGradingPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
