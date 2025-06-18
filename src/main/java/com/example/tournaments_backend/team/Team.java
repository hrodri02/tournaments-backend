package com.example.tournaments_backend.team;

import java.util.HashSet;
import java.util.Set;

import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Team {
    @Id
    @SequenceGenerator(
        name="team_sequence",
        sequenceName="team_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "team_sequence"
    )
    @EqualsAndHashCode.Include
    private Long id;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "Team name must be between 2 and 255 characters.")
    private String name;
    @JsonIgnore
    @ManyToMany(mappedBy = "teams")
    private Set<League> leagues = new HashSet<>();
    @OneToMany(mappedBy = "team")
    @JsonManagedReference
    private Set<Player> players; 

    public Team(String name) {
        this.name = name;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        player.setTeam(this);
    }
}
