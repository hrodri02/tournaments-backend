package com.example.tournaments_backend.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.AbstractIntegrationTest;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;

@Transactional
public class PlayerServiceIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    private Player buildPlayer(String email) {
        return new Player(
            "John",
            "Doe",
            email,
            "password",
            AppUserRole.PLAYER,
            Position.STRIKER
        );
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void save_ShouldPersistPlayerWithGeneratedId_WhenPlayerIsValid() {
        Player player = buildPlayer("jdoe@test.com");

        Player result = playerService.save(player);

        assertNotNull(result.getId());
        assertThat(playerRepository.findById(result.getId())).isPresent();
    }

    // ─── getPlayerDTOById ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getPlayerDTOById_ShouldReturnPlayerDTO_WhenPlayerExists() {
        Player saved = playerRepository.save(buildPlayer("jdoe@test.com"));

        PlayerDTO result = playerService.getPlayerDTOById(saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("jdoe@test.com");
        assertThat(result.getPosition()).isEqualTo(Position.STRIKER);
    }

    @Test
    void getPlayerDTOById_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        assertThatThrownBy(() -> playerService.getPlayerDTOById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── getPlayerById ────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getPlayerById_ShouldReturnPlayer_WhenPlayerExists() {
        Player saved = playerRepository.save(buildPlayer("jdoe@test.com"));

        Player result = playerService.getPlayerById(saved.getId());

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getPlayerById_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        assertThatThrownBy(() -> playerService.getPlayerById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── getPlayerByEmail ─────────────────────────────────────────────────────

    @Test
    void getPlayerByEmail_ShouldReturnPlayer_WhenPlayerExists() {
        Player saved = playerRepository.save(buildPlayer("jdoe@test.com"));

        Player result = playerService.getPlayerByEmail("jdoe@test.com");

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getPlayerByEmail_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        assertThatThrownBy(() -> playerService.getPlayerByEmail("missing@test.com"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── deletePlayerById ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void deletePlayerById_ShouldRemovePlayerFromDb_WhenPlayerExists() {
        Player saved = playerRepository.save(buildPlayer("jdoe@test.com"));

        playerService.deletePlayerById(saved.getId());

        assertThat(playerRepository.findById(saved.getId())).isEmpty();
    }

    // ─── updatePlayer ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void updatePlayer_ShouldPersistUpdatedFields_WhenPlayerExists() {
        Player saved = playerRepository.save(buildPlayer("jdoe@test.com"));
        PlayerDTO updateRequest = new PlayerDTO(
            null,
            "Jane",
            "Smith",
            "jane.smith@test.com",
            AppUserRole.PLAYER,
            Position.GOAL_KEEPER
        );

        PlayerDTO result = playerService.updatePlayer(saved.getId(), updateRequest);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getEmail()).isEqualTo("jane.smith@test.com");
        assertThat(result.getPosition()).isEqualTo(Position.GOAL_KEEPER);

        Player inDb = playerRepository.findById(saved.getId()).get();
        assertThat(inDb.getFirstName()).isEqualTo("Jane");
        assertThat(inDb.getLastName()).isEqualTo("Smith");
        assertThat(inDb.getEmail()).isEqualTo("jane.smith@test.com");
        assertThat(inDb.getPosition()).isEqualTo(Position.GOAL_KEEPER);
    }

    @Test
    void updatePlayer_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        PlayerDTO updateRequest = new PlayerDTO(
            null,
            "Jane",
            "Smith",
            "jane.smith@test.com",
            AppUserRole.PLAYER,
            Position.GOAL_KEEPER
        );

        assertThatThrownBy(() -> playerService.updatePlayer(-1L, updateRequest))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── getAllPlayersByEmail ─────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getAllPlayersByEmail_ShouldReturnMatchingPlayers_WhenEmailsExist() {
        Player player1 = playerRepository.save(buildPlayer("player1@test.com"));
        Player player2 = playerRepository.save(buildPlayer("player2@test.com"));

        List<Player> result = playerService.getAllPlayersByEmail(
            List.of("player1@test.com", "player2@test.com")
        );

        assertThat(result).containsExactlyInAnyOrder(player1, player2);
    }

    @Test
    void getAllPlayersByEmail_ShouldReturnEmptyList_WhenNoEmailsMatch() {
        List<Player> result = playerService.getAllPlayersByEmail(List.of("missing@test.com"));

        assertThat(result).isEmpty();
    }

    // ─── getAllPlayersByIds ───────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getAllPlayersByIds_ShouldReturnMatchingPlayers_WhenIdsExist() {
        Player player1 = playerRepository.save(buildPlayer("player1@test.com"));
        Player player2 = playerRepository.save(buildPlayer("player2@test.com"));

        List<Player> result = playerService.getAllPlayersByIds(
            List.of(player1.getId(), player2.getId())
        );

        assertThat(result).containsExactlyInAnyOrder(player1, player2);
    }

    @Test
    void getAllPlayersByIds_ShouldReturnEmptyList_WhenNoIdsMatch() {
        List<Player> result = playerService.getAllPlayersByIds(List.of(-1L));

        assertThat(result).isEmpty();
    }
}
