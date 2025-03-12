package com.example.tournaments_backend.app_user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppUserConfig {
    
    @Bean
    CommandLineRunner commandLineRunner(AppUserRepository appUserRepository) {
        return args -> {
            // Person heri = new Person(
            // "Heriberto",
            // "Rodriguez",
            // "hrodriguez@gmail.com",
            // "djiaogdb");
            
            // Person bray = new Person(
            // "Brayan",
            // "Rodriguez",
            // "brodriguez@gmail.com",
            // "idjgioadjo");

            // personRepository.saveAll(List.of(heri, bray));
        };
    }
}
