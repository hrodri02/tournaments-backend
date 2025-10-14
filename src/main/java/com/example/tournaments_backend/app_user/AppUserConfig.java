package com.example.tournaments_backend.app_user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
            Set<Player> team1Players = createPlayersForTeam1(passwordEncoder);
            team1.addPlayers(team1Players);
            team1.setOwner(getPlayerWithFirstName(team1Players, "Sarah"));
            Team team2 = new Team("Real Madrid");
            Set<Player> team2Players = createPlayersForTeam2(passwordEncoder);
            team2.addPlayers(team2Players);
            team2.setOwner(getPlayerWithFirstName(team2Players, "Alice"));
            Team team3 = new Team("Manchester United");
            Set<Player> team3Players = createPlayersForTeam3();
            team3.setOwner(getPlayerWithFirstName(team3Players, "Liam"));
            team3.addPlayers(team3Players);
            Team team4 = new Team("Liverpool");
            Set<Player> team4Players = createPlayersForTeam4();
            team4.setOwner(getPlayerWithFirstName(team4Players, "Willow"));
            team4.addPlayers(team4Players);
            Team bayern = new Team("Bayern Munich");
            Set<Player> bayernPlayers = createPlayersForBayern();
            bayern.addPlayers(bayernPlayers);
            bayern.setOwner(getPlayerWithFirstName(bayernPlayers, "Liam"));
            Team borussia = new Team("Borussia Dortmund");
            Set<Player> borussiaPlayers = createPlayersForBorussia();
            borussia.addPlayers(borussiaPlayers);
            borussia.setOwner(getPlayerWithFirstName(borussiaPlayers, "Henry"));
            Team america = new Team("América");
            Set<Player> americaPlayers = createPlayersForAmerica();
            america.setOwner(getPlayerWithFirstName(americaPlayers, "Michael"));
            america.addPlayers(americaPlayers);
            Team tigres = new Team("Tigres");
            Set<Player> tigresPlayers = createPlayersForTigres();
            tigres.setOwner(getPlayerWithFirstName(tigresPlayers, "Ronald"));
            tigres.addPlayers(tigresPlayers);

            teamRepository.saveAll(Arrays.asList(team1, team2, team3, team4, bayern, borussia, america, tigres));

            // 1. Get the ZoneId for San Francisco
            ZoneId sanFranciscoZone = ZoneId.of("America/Los_Angeles");

            // 2. Get the current ZonedDateTime for that time zone
            ZonedDateTime sanFranciscoZonedTime = ZonedDateTime.now(sanFranciscoZone);

            LocalDateTime sanFranciscoLocalDateTime = sanFranciscoZonedTime.toLocalDateTime();
            LocalDate sanFranciscoLocalDate = sanFranciscoZonedTime.toLocalDate();

            // Create and save leagues (not started)
            League league1 = new League("La Liga", sanFranciscoLocalDate.plusDays(7), 12);
            league1.addTeam(team1);
            league1.addTeam(team2);

            League league2 = new League("Premier League", sanFranciscoLocalDate.plusDays(14), 10);
            league2.addTeam(team3);
            league2.addTeam(team4);

            // Create league that has ended
            League league3 = new League("Bundes Liga", sanFranciscoLocalDate.minusWeeks(13), 12);
            league3.addTeam(bayern);
            league3.addTeam(borussia);

            // Create league that is in progress
            League league4 = new League("Liga MX", sanFranciscoLocalDate.minusWeeks(6), 10);
            league4.addTeam(america);
            league4.addTeam(tigres);

            leagueRepository.saveAll(Arrays.asList(league1, league2, league3, league4));

            // games for league 1
            Game game1 = new Game(sanFranciscoLocalDateTime.plusDays(7), 
                                "Golden Gate Park",
                                90);
            game1.setHomeTeam(team1);
            game1.setAwayTeam(team2);
            game1.setLeague(league1);

            Game game2 = new Game(sanFranciscoLocalDateTime.plusDays(12), 
                                "Garfield Park",
                                90);
            game2.setHomeTeam(team2);
            game2.setAwayTeam(team1);
            game2.setLeague(league1);

            // games for league 2
            Game game3 = new Game(sanFranciscoLocalDateTime.plusDays(14), 
                                "Silver Park",
                                90);
            game3.setHomeTeam(team3);
            game3.setAwayTeam(team4);
            game3.setLeague(league2);

            Game game4 = new Game(sanFranciscoLocalDateTime.plusDays(19), 
                                "Mission Park",
                                90);
            game4.setHomeTeam(team4);
            game4.setAwayTeam(team3);
            game4.setLeague(league2);

            // games for league 3 (started 13 weeks ago)
            Game game5 = new Game(sanFranciscoLocalDateTime.minusWeeks(12), 
                                "Allianz Arena",
                                90);
            game5.setHomeTeam(bayern);
            game5.setAwayTeam(borussia);
            game5.setLeague(league3);

            Game game6 = new Game(sanFranciscoLocalDateTime.minusWeeks(11), 
                                "Signal Iduna Park",
                                90);
            game6.setHomeTeam(borussia);
            game6.setAwayTeam(bayern);
            game6.setLeague(league3);

            // Games for league 4 that is in progress
            Game game7 = new Game(sanFranciscoLocalDateTime.minusWeeks(6), 
                                "Estadio Azteca",
                                90);
            game7.setHomeTeam(america);
            game7.setAwayTeam(tigres);
            game7.setLeague(league4);

            Game game8 = new Game(sanFranciscoLocalDateTime.minusWeeks(5), 
                                "Estadio Universitario",
                                90);
            game8.setHomeTeam(tigres);
            game8.setAwayTeam(america);
            game8.setLeague(league4);

            // added an active game for testing
            Game game9 = new Game(sanFranciscoLocalDateTime, 
                                "Estadio Azteca",
                                90);
            game9.setHomeTeam(america);
            game9.setAwayTeam(tigres);
            game9.setLeague(league4);

            gameRepository.saveAll(List.of(game1, game2, game3, game4, game5, game6, game7, game8, game9));

            // stats for game5
            GameStat gameStat1 = new GameStat(GameStatType.GOAL, sanFranciscoLocalDateTime.minusWeeks(12).plusMinutes(70));
            gameStat1.setGame(game5);
            Player isabella = getPlayerWithFirstName(bayernPlayers, "Isabella");
            gameStat1.setPlayer(isabella);
            
            // stats for game6
            GameStat gameStat2 = new GameStat(GameStatType.GOAL, sanFranciscoLocalDateTime.minusWeeks(11).plusMinutes(30));
            gameStat2.setGame(game6);
            Player abigail = getPlayerWithFirstName(borussiaPlayers, "Abigail");
            gameStat2.setPlayer(abigail);
            
            // stats for game7
            GameStat gameStat3 = new GameStat(GameStatType.YELLOW_CARD, sanFranciscoLocalDateTime.minusWeeks(6).plusMinutes(50));
            gameStat3.setGame(game7);
            Player harry = getPlayerWithFirstName(tigresPlayers, "Harry");
            gameStat3.setPlayer(harry);

            // stats for game8
            GameStat gameStat4 = new GameStat(GameStatType.YELLOW_CARD, sanFranciscoLocalDateTime.minusWeeks(5).plusMinutes(60));
            gameStat4.setGame(game8);
            Player angela = getPlayerWithFirstName(americaPlayers, "Angela");
            gameStat4.setPlayer(angela);

            gameStatRepository.saveAll(List.of(gameStat1, gameStat2, gameStat3, gameStat4));
            System.out.println("Database initialized with mock data!");
        };
    }

    public Set<Player> createPlayersForTeam1(BCryptPasswordEncoder passwordEncoder) {
        Set<Player> players = new HashSet<>();
        // Defenders
        players.add(new Player("Sarah", "Connor", "sconnor@example.com", passwordEncoder.encode("securepass1"), AppUserRole.PLAYER, Position.DEFENDER));
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

    public Set<Player> createPlayersForTeam2(BCryptPasswordEncoder passwordEncoder) {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Alice", "Wonderland", "alice.w@example.com", passwordEncoder.encode("passT2D1"), AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Bob", "Thebuilder", "bob.t@example.com", passwordEncoder.encode("passT2D2"), AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Charlie", "Chaplin", "charlie.c@example.com", passwordEncoder.encode("passT2D3"), AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Diana", "Prince", "diana.p@example.com", passwordEncoder.encode("passT2D4"), AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Ethan", "Hunt", "ethan.h@example.com", passwordEncoder.encode("passT2M1"), AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Fiona", "Shrek", "fiona.s@example.com", passwordEncoder.encode("passT2M2"), AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("George", "Jungle", "george.j@example.com", passwordEncoder.encode("passT2M3"), AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Hannah", "Montana", "hannah.m@example.com", passwordEncoder.encode("passT2M4"), AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Ivan", "Theterrible", "ivan.t@example.com", passwordEncoder.encode("passT2GK"), AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Julia", "Roberts", "julia.r@example.com", passwordEncoder.encode("passT2S1"), AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Kevin", "Hart", "kevin.h@example.com", passwordEncoder.encode("passT2S2"), AppUserRole.PLAYER, Position.STRIKER));
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

    public Set<Player> createPlayersForBayern() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Liam", "Smith", "liam.s@example.com", "passT3D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Mia", "Jones", "mia.j@example.com", "passT3D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Noah", "Brown", "noah.b@example.com", "passT3D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Olivia", "Davis", "olivia.d@example.com", "passT3D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("James", "Miller", "james.m@example.com", "passT3M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Sophia", "Garcia", "sophia.g@example.com", "passT3M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Benjamin", "Rodriguez", "benjamin.r@example.com", "passT3M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Emma", "Martinez", "emma.m@example.com", "passT3M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("William", "Hernandez", "william.h@example.com", "passT3GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Isabella", "Lopez", "isabella.l@example.com", "passT3S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Lucas", "Gonzalez", "lucas.g@example.com", "passT3S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForBorussia() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Henry", "Wilson", "henry.w@example.com", "passT4D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Charlotte", "Anderson", "charlotte.a@example.com", "passT4D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Alexander", "Thomas", "alexander.t@example.com", "passT4D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Amelia", "Jackson", "amelia.j@example.com", "passT4D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Daniel", "White", "daniel.w@example.com", "passT4M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Harper", "Harris", "harper.h@example.com", "passT4M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Matthew", "Martin", "matthew.m@example.com", "passT4M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Evelyn", "Thompson", "evelyn.t@example.com", "passT4M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Samuel", "Moore", "samuel.m@example.com", "passT4GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Abigail", "Young", "abigail.y@example.com", "passT4S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("David", "King", "david.k@example.com", "passT4S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForAmerica() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Michael", "Scott", "michael.s@example.com", "passT5D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Pamela", "Beesly", "pamela.b@example.com", "passT5D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Dwight", "Schrute", "dwight.s@example.com", "passT5D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Angela", "Martin", "angela.m@example.com", "passT5D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Jim", "Halpert", "jim.h@example.com", "passT5M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Phyllis", "Lapin", "phyllis.l@example.com", "passT5M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Stanley", "Hudson", "stanley.h@example.com", "passT5M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Oscar", "Martinez", "oscar.m@example.com", "passT5M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("Kevin", "Malone", "kevin.m@example.com", "passT5GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Erin", "Hannon", "erin.h@example.com", "passT5S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Andy", "Bernard", "andy.b@example.com", "passT5S2", AppUserRole.PLAYER, Position.STRIKER));
        return players;
    }

    public Set<Player> createPlayersForTigres() {
        Set<Player> players = new HashSet<>();
        // Defenders (4)
        players.add(new Player("Ronald", "Weasley", "ronald.w@example.com", "passT6D1", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Hermione", "Granger", "hermione.g@example.com", "passT6D2", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Harry", "Potter", "harry.p@example.com", "passT6D3", AppUserRole.PLAYER, Position.DEFENDER));
        players.add(new Player("Ginny", "Weasley", "ginny.w@example.com", "passT6D4", AppUserRole.PLAYER, Position.DEFENDER));
        // Midfielders (4)
        players.add(new Player("Draco", "Malfoy", "draco.m@example.com", "passT6M1", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Luna", "Lovegood", "luna.l@example.com", "passT6M2", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Neville", "Longbottom", "neville.l@example.com", "passT6M3", AppUserRole.PLAYER, Position.MIDFIELDER));
        players.add(new Player("Fred", "Weasley", "fred.w@example.com", "passT6M4", AppUserRole.PLAYER, Position.MIDFIELDER));
        // Goalkeeper (1)
        players.add(new Player("George", "Weasley", "george.w@example.com", "passT6GK", AppUserRole.PLAYER, Position.GOAL_KEEPER));
        // Strikers (2)
        players.add(new Player("Severus", "Snape", "severus.s@example.com", "passT6S1", AppUserRole.PLAYER, Position.STRIKER));
        players.add(new Player("Minerva", "McGonagall", "minerva.m@example.com", "passT6S2", AppUserRole.PLAYER, Position.STRIKER));
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
