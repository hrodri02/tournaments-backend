package com.example.tournaments_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class TournamentsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TournamentsBackendApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry
					.addMapping("/api/v*/**")
                    .allowedOrigins("http://localhost:8081")
					.exposedHeaders("Authorization");
			}
		};
	}
}
