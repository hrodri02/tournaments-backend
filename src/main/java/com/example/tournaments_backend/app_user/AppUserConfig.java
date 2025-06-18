package com.example.tournaments_backend.app_user;

import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueRepository;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamRepository;

@Configuration
public class AppUserConfig {

    @Bean
    CommandLineRunner commandLineRunner(
            AppUserRepository appUserRepository, 
            LeagueRepository leagueRepository,
            TeamRepository teamRepository,
            BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // Create and save users
            AppUser user = new AppUser(
                "User",
                "Example",
                "user@example.com",
                passwordEncoder.encode("password"),
                AppUserRole.USER
            );
            user.setEnabled(true); // Enable the user so they can log in

            AppUser admin = new AppUser(
                "Admin",
                "User",
                "admin@example.com",
                passwordEncoder.encode("admin123"),
                AppUserRole.ADMIN
            );
            admin.setEnabled(true);

            appUserRepository.saveAll(Arrays.asList(user, admin));

            // Create and save teams
            Team team1 = new Team("Barcelona FC");
            Team team2 = new Team("Real Madrid");
            Team team3 = new Team("Manchester United");
            Team team4 = new Team("Liverpool");

            teamRepository.saveAll(Arrays.asList(team1, team2, team3, team4));

            // Create and save leagues
            League league1 = new League("La Liga", LocalDate.now().plusDays(7), 12);
            league1.addTeam(team1);
            league1.addTeam(team2);

            League league2 = new League("Premier League", LocalDate.now().plusDays(14), 10);
            league2.addTeam(team3);
            league2.addTeam(team4);

            leagueRepository.saveAll(Arrays.asList(league1, league2));

            System.out.println("Database initialized with mock data!");
        };
    }
}
