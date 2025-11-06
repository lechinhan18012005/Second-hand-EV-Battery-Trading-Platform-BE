package com.evdealer.evdealermanagement;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.evdealer.evdealermanagement.repository")
@EnableRedisRepositories(
		basePackages = "com.evdealer.evdealermanagement.redis",
		enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.OFF,
		considerNestedRepositories = false
)
@EnableAsync
@EnableScheduling
public class EvDealerManagementSystemApplication {

	public static void main(String[] args) {
		// Chỉ load .env nếu không chạy test
		if (!isTestEnvironment()) {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing()
					.load();

			dotenv.entries().forEach(entry ->
					System.setProperty(entry.getKey(), entry.getValue())
			);
		}

		SpringApplication.run(EvDealerManagementSystemApplication.class, args);
	}

	private static boolean isTestEnvironment() {
		String[] activeProfiles = System.getProperty("spring.profiles.active", "").split(",");
		for (String profile : activeProfiles) {
			if (profile.trim().equalsIgnoreCase("test")) return true;
		}
		return false;
	}
}
