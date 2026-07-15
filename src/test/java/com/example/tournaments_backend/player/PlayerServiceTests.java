package com.example.tournaments_backend.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
public class PlayerServiceTests {
    @Mock
    private PlayerRepository playerRepository;
    @InjectMocks
    private PlayerService playerService;

    private Player buildPlayer() {
        return new Player(
            "John",
            "Doe",
            "jdoe@example.com",
            "password",
            AppUserRole.PLAYER,
            Position.STRIKER
        );
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_ShouldReturnSavedPlayer_WhenPlayerIsValid() {
        // 1. Arrange
        Player player = buildPlayer();

        when(playerRepository.save(player)).thenReturn(player);

        // 2. Act
        Player result = playerService.save(player);

        // 3. Assert
        assertThat(result).isEqualTo(player);
        verify(playerRepository).save(player);
    }

    // ─── getPlayerDTOById ─────────────────────────────────────────────────────

    @Test
    void getPlayerDTOById_ShouldReturnPlayerDTO_WhenPlayerExists() {
        // 1. Arrange
        Player player = buildPlayer();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        // 2. Act
        PlayerDTO result = playerService.getPlayerDTOById(1L);

        // 3. Assert
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(result.getPosition()).isEqualTo(Position.STRIKER);
    }

    @Test
    void getPlayerDTOById_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        // 1. Arrange
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerDTOById(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── getPlayerById ────────────────────────────────────────────────────────

    @Test
    void getPlayerById_ShouldReturnPlayer_WhenPlayerExists() {
        // 1. Arrange
        Player player = buildPlayer();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        // 2. Act
        Player result = playerService.getPlayerById(1L);

        // 3. Assert
        assertThat(result).isEqualTo(player);
    }

    @Test
    void getPlayerById_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        // 1. Arrange
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerById(1L))
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
        // 1. Arrange
        Player player = buildPlayer();

        when(playerRepository.findByEmail("jdoe@example.com")).thenReturn(Optional.of(player));

        // 2. Act
        Player result = playerService.getPlayerByEmail("jdoe@example.com");

        // 3. Assert
        assertThat(result).isEqualTo(player);
    }

    @Test
    void getPlayerByEmail_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        // 1. Arrange
        when(playerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerByEmail("missing@example.com"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });
    }

    // ─── deletePlayerById ─────────────────────────────────────────────────────

    @Test
    void deletePlayerById_ShouldCallRepositoryDeleteById() {
        // 1. Act
        playerService.deletePlayerById(1L);

        // 2. Assert
        verify(playerRepository).deleteById(1L);
    }

    // ─── updatePlayer ─────────────────────────────────────────────────────────

    @Test
    void updatePlayer_ShouldUpdateAndReturnPlayerDTO_WhenPlayerExists() {
        // 1. Arrange
        Player existingPlayer = buildPlayer();
        PlayerDTO updateRequest = new PlayerDTO(
            null,
            "Jane",
            "Smith",
            "jane.smith@example.com",
            AppUserRole.PLAYER,
            Position.GOAL_KEEPER
        );

        when(playerRepository.findById(1L)).thenReturn(Optional.of(existingPlayer));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Act
        PlayerDTO result = playerService.updatePlayer(1L, updateRequest);

        // 3. Assert
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(result.getPosition()).isEqualTo(Position.GOAL_KEEPER);

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(playerCaptor.capture());
        assertThat(playerCaptor.getValue().getFirstName()).isEqualTo("Jane");
        assertThat(playerCaptor.getValue().getLastName()).isEqualTo("Smith");
        assertThat(playerCaptor.getValue().getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(playerCaptor.getValue().getPosition()).isEqualTo(Position.GOAL_KEEPER);
    }

    @Test
    void updatePlayer_ShouldThrowServiceException_WhenPlayerDoesNotExist() {
        // 1. Arrange
        PlayerDTO updateRequest = new PlayerDTO(
            null,
            "Jane",
            "Smith",
            "jane.smith@example.com",
            AppUserRole.PLAYER,
            Position.GOAL_KEEPER
        );

        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> playerService.updatePlayer(1L, updateRequest))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.USER_NOT_FOUND);
                });

        verify(playerRepository, never()).save(any());
    }

    // ─── getAllPlayersByEmail ─────────────────────────────────────────────────

    @Test
    void getAllPlayersByEmail_ShouldReturnMatchingPlayers_WhenEmailsExist() {
        // 1. Arrange
        Player player = buildPlayer();
        List<String> emails = List.of("jdoe@example.com");

        when(playerRepository.findAllByEmailIn(emails)).thenReturn(List.of(player));

        // 2. Act
        List<Player> result = playerService.getAllPlayersByEmail(emails);

        // 3. Assert
        assertThat(result).containsExactly(player);
        verify(playerRepository).findAllByEmailIn(emails);
    }

    @Test
    void getAllPlayersByEmail_ShouldReturnEmptyList_WhenNoEmailsMatch() {
        // 1. Arrange
        List<String> emails = List.of("missing@example.com");

        when(playerRepository.findAllByEmailIn(emails)).thenReturn(List.of());

        // 2. Act
        List<Player> result = playerService.getAllPlayersByEmail(emails);

        // 3. Assert
        assertThat(result).isEmpty();
    }

    // ─── getAllPlayersByIds ───────────────────────────────────────────────────

    @Test
    void getAllPlayersByIds_ShouldReturnMatchingPlayers_WhenIdsExist() {
        // 1. Arrange
        Player player = buildPlayer();
        List<Long> ids = List.of(1L);

        when(playerRepository.findAllById(ids)).thenReturn(List.of(player));

        // 2. Act
        List<Player> result = playerService.getAllPlayersByIds(ids);

        // 3. Assert
        assertThat(result).containsExactly(player);
        verify(playerRepository).findAllById(ids);
    }

    @Test
    void getAllPlayersByIds_ShouldReturnEmptyList_WhenNoIdsMatch() {
        // 1. Arrange
        List<Long> ids = List.of(999L);

        when(playerRepository.findAllById(ids)).thenReturn(List.of());

        // 2. Act
        List<Player> result = playerService.getAllPlayersByIds(ids);

        // 3. Assert
        assertThat(result).isEmpty();
    }
}
