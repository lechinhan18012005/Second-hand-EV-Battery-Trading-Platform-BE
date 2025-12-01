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
		// Load environment variables từ .env file (chỉ cho local dev)
		loadEnvironmentVariables();

		SpringApplication.run(EvDealerManagementSystemApplication.class, args);
	}

	private static void loadEnvironmentVariables() {
		// Skip nếu đang chạy test
		if (isTestEnvironment()) {
			System.out.println("Test environment detected, skipping .env file loading");
			return;
		}

		// Skip nếu đang chạy trên production/Railway (có biến môi trường PORT)
		if (isProductionEnvironment()) {
			System.out.println("Production environment detected, using system environment variables");
			return;
		}

		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing()
					.load();

			// Load tất cả biến từ .env vào System properties
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});

			System.out.println("Successfully loaded .env file for local development");
		} catch (Exception e) {
			System.out.println("Warning: Could not load .env file - " + e.getMessage());
			System.out.println("Continuing with system environment variables...");
		}
	}

	private static boolean isTestEnvironment() {
		// Check Spring profiles
		String activeProfiles = System.getProperty("spring.profiles.active", "");
		if (activeProfiles.contains("test")) {
			return true;
		}

		// Check environment variable
		String springProfile = System.getenv("SPRING_PROFILES_ACTIVE");
		return springProfile != null && springProfile.contains("test");
	}

	private static boolean isProductionEnvironment() {
		// Railway và hầu hết cloud platforms set biến PORT
		String port = System.getenv("PORT");

		// Hoặc check Railway-specific environment variables
		String railwayEnv = System.getenv("RAILWAY_ENVIRONMENT");

		// Hoặc check production profile
		String activeProfiles = System.getProperty("spring.profiles.active", "");
		String springProfile = System.getenv("SPRING_PROFILES_ACTIVE");

		return port != null
				|| railwayEnv != null
				|| activeProfiles.contains("prod")
				|| activeProfiles.contains("production")
				|| (springProfile != null && (springProfile.contains("prod") || springProfile.contains("production")));
	}
}