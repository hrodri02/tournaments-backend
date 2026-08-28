package com.example.tournaments_backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.ContainerConfig.class)
public abstract class AbstractIntegrationTest {

    /**
     * The container is a bean rather than a static field: Boot starts and stops
     * it with the application context, and {@code @ServiceConnection} binds it
     * to the datasource, so no test registers {@code spring.datasource.*} by
     * hand. Every subclass shares one context, and therefore one container.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
