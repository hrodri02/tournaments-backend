package com.example.tournaments_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        // Could be improved by moving this to env variables.
        devServer.setUrl(baseUrl);
        devServer.setDescription("Server URL in Development environment");

        Info info = new Info()
                .title("Tournaments API")
                .version("0.1")
                .description("This API exposes endpoints to manage tournaments, teams, leagues, and users.");

        // JWT security scheme definiton
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security scheme to the components
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", jwtScheme);

        // Security requirement for all endpoints (except those explicitly permitted in SecurityConfig)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}
