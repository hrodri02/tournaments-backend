package com.example.tournaments_backend.app_user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.game.GameRepository;
import com.example.tournaments_backend.game_stat.GameStat;
import com.example.tournaments_backend.game_stat.GameStatRepository;
import com.example.tournaments_backend.game_stat.GameStatType;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueRepository;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerRepository;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamRepository;

@Configuration
public class AppUserConfig {

    @Bean
    CommandLineRunner commandLineRunner(
            AppUserRepository appUserRepository, 
            PlayerRepository playerRepository,
            LeagueRepository leagueRepository,
            TeamRepository teamRepository,
            GameRepository gameRepository,
            GameStatRepository gameStatRepository,
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
            Set<Player> team1Players = createPlayersForTeam1();
            team1.addPlayers(team1Players);
            Team team2 = new Team("Real Madrid");
            Set<Player> team2Players = createPlayersForTeam2();
            team2.addPlayers(team2Players);
            Team team3 = new Team("Manchester United");
            Set<Player> team3Players = createPlayersForTeam3();
            team3.addPlayers(team3Players);
            Team team4 = new Team("Liverpool");
            Set<Player> team4Players = createPlayersForTeam4();
            team4.addPlayers(team4Players);

            teamRepository.saveAll(Arrays.asList(team1, team2, team3, team4));

            // Create and save leagues
            League league1 = new League("La Liga", LocalDate.now().plusDays(7), 12);
            league1.addTeam(team1);
            league1.addTeam(team2);

            League league2 = new League("Premier League", LocalDate.now().plusDays(14), 10);
            league2.addTeam(team3);
            league2.addTeam(team4);

            leagueRepository.saveAll(Arrays.asList(league1, league2));

            Game game1 = new Game(LocalDateTime.now().plusDays(7), 
                                "Golden Gate Park",
                                90);
            game1.setHomeTeam(team1);
            game1.setAwayTeam(team2);
            game1.setLeague(league1);

            Game game2 = new Game(LocalDateTime.now().plusDays(12), 
                                "Garfield Park",
                                90);
            game2.setHomeTeam(team2);
            game2.setAwayTeam(team1);
            game2.setLeague(league1);

            Game game3 = new Game(LocalDateTime.now().plusDays(14), 
                                "Silver Park",
                                90);
            game3.setHomeTeam(team3);
            game3.setAwayTeam(team4);
            game3.setLeague(league2);

            Game game4 = new Game(LocalDateTime.now().plusDays(19), 
                                "Mission Park",
                                90);
            game4.setHomeTeam(team4);
            game4.setAwayTeam(team3);
            game4.setLeague(league2);
            gameRepository.saveAll(List.of(game1, game2, game3, game4));

            // stats for game1
            GameStat gameStat1 = new GameStat(GameStatType.GOAL, LocalDateTime.now().plusDays(7).plusMinutes(25));
            gameStat1.setGame(game1);
            Player raul = getPlayerWithFirstName(team1Players, "Raul");
            gameStat1.setPlayer(raul);
            GameStat gameStat2 = new GameStat(GameStatType.GOAL, LocalDateTime.now().plusDays(7).plusMinutes(35));
            gameStat2.setGame(game1);
            gameStat2.setPlayer(raul);

            // stats for game2
            GameStat gameStat3 = new GameStat(GameStatType.YELLOW_CARD, LocalDateTime.now().plusDays(12).plusMinutes(25));
            gameStat3.setGame(game2);
            Player alice = getPlayerWithFirstName(team2Players, "Alice");
            gameStat3.setPlayer(alice);
            GameStat gameStat4 = new GameStat(GameStatType.YELLOW_CARD, LocalDateTime.now().plusDays(12).plusMinutes(35));
            gameStat4.setGame(game2);
            Player laura = getPlayerWithFirstName(team1Players, "Laura");
            gameStat4.setPlayer(laura);

            // stats for game3
            GameStat gameStat5 = new GameStat(GameStatType.RED_CARD, LocalDateTime.now().plusDays(14).plusMinutes(25));
            gameStat5.setGame(game3);
            Player liam = getPlayerWithFirstName(team3Players, "Liam");
            gameStat5.setPlayer(liam);
            GameStat gameStat6 = new GameStat(GameStatType.GOAL, LocalDateTime.now().plusDays(14).plusMinutes(35));
            gameStat6.setGame(game3);
            Player frank = getPlayerWithFirstName(team4Players, "Frank");
            gameStat6.setPlayer(frank);

            // stats for game4
            GameStat gameStat7 = new GameStat(GameStatType.RED_CARD, LocalDateTime.now().plusDays(12).plusMinutes(25));
            gameStat7.setGame(game4);
            Player mia = getPlayerWithFirstName(team3Players, "Mia");
            gameStat7.setPlayer(mia);
            GameStat gameStat8 = new GameStat(GameStatType.RED_CARD, LocalDateTime.now().plusDays(12).plusMinutes(35));
            gameStat8.setGame(game4);
            Player willow = getPlayerWithFirstName(team4Players, "Willow");
            gameStat8.setPlayer(willow);

            gameStatRepository.saveAll(List.of(gameStat1, gameStat2, gameStat3, gameStat4, gameStat5, gameStat6, gameStat7, gameStat8));
            System.out.println("Database initialized with mock data!");
        };
    }

    public Set<Player> createPlayersForTeam1() {
        Set<Player> players = new HashSet<>();
        // Defenders
        players.add(new Player("Sarah", "Connor", "sconnor@example.com", "securepass1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("David", "Silva", "dsilva@example.com", "securepass2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Laura", "Smith", "lsmith@example.com", "securepass3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Michael", "Johnson", "mjohnson@example.com", "securepass4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders
        players.add(new Player("Emily", "White", "ewhite@example.com", "securepass5", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Chris", "Brown", "cbrown@example.com", "securepass6", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Olivia", "Green", "ogreen@example.com", "securepass7", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Daniel", "Black", "dblack@example.com", "securepass8", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper
        players.add(new Player("Sophia", "Miller", "smiller@example.com", "securepass9", AppUserRole.PLAYER, Position.GOAL_KEEPER)); // Only one goalkeeper
        // Strikers 
        players.add(new Player("James", "Rodriguez", "jrodriguez@example.com", "securepass10", AppUserRole.PLAYER, Position.STRIKER)); // 2nd Striker
        players.add(new Player("Raul", "Jimenez", "rjimenez@gmail.com", "aseavdabdaf", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForTeam2() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Alice", "Wonderland", "alice.w@example.com", "passT2D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Bob", "Thebuilder", "bob.t@example.com", "passT2D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Charlie", "Chaplin", "charlie.c@example.com", "passT2D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Diana", "Prince", "diana.p@example.com", "passT2D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Ethan", "Hunt", "ethan.h@example.com", "passT2M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Fiona", "Shrek", "fiona.s@example.com", "passT2M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("George", "Jungle", "george.j@example.com", "passT2M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Hannah", "Montana", "hannah.m@example.com", "passT2M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Ivan", "Theterrible", "ivan.t@example.com", "passT2GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Julia", "Roberts", "julia.r@example.com", "passT2S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Kevin", "Hart", "kevin.h@example.com", "passT2S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForTeam3() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Liam", "Neeson", "liam.n@example.com", "passT3D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Mia", "Khalifa", "mia.k@example.com", "passT3D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Noah", "Centineo", "noah.c@example.com", "passT3D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Olga", "Korbut", "olga.k@example.com", "passT3D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Peter", "Pan", "peter.p@example.com", "passT3M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Quinn", "Fabray", "quinn.f@example.com", "passT3M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Rachel", "Green", "rachel.g@example.com", "passT3M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Steve", "Rogers", "steve.r@example.com", "passT3M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Tina", "Turner", "tina.t@example.com", "passT3GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Ursula", "Sea", "ursula.s@example.com", "passT3S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Vince", "Vaughn", "vince.v@example.com", "passT3S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForTeam4() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Willow", "Smith", "willow.s@example.com", "passT4D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Xavier", "School", "xavier.x@example.com", "passT4D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Yara", "Shahidi", "yara.s@example.com", "passT4D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Zack", "Galifianakis", "zack.g@example.com", "passT4D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Amy", "Adams", "amy.a@example.com", "passT4M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Ben", "Affleck", "ben.a@example.com", "passT4M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Chloe", "Grace", "chloe.g@example.com", "passT4M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Drew", "Barrymore", "drew.b@example.com", "passT4M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Emma", "Stone", "emma.s@example.com", "passT4GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Frank", "Ocean", "frank.o@example.com", "passT4S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Gigi", "Hadid", "gigi.h@example.com", "passT4S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    private Player getPlayerWithFirstName(Set<Player> players, String firstName) {
        for (Player player : players) {
            if (player.getFirstName().equals(firstName)) {
                return player;
            }
        }
        return null;
    }
}
