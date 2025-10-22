package com.example.tournaments_backend.team;

import java.util.HashSet;
import java.util.Set;

import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Player owner;
    private String name;
    @Column(name = "logo_url")
    private String logoUrl;
    @JsonIgnore
    @ManyToMany(mappedBy = "teams")
    private Set<League> leagues = new HashSet<>();
    @JsonIgnore 
    @ManyToMany(mappedBy = "teams", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Player> players = new HashSet<>();;
    @OneToMany(mappedBy = "homeTeam", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Game> homeGames;
    @OneToMany(mappedBy = "awayTeam", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Game> awayGames;

    public Team(String name) {
        this.name = name;
    }

    public Team(TeamRequest teamRequest) {
        this.name = teamRequest.getName();
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        player.getTeams().add(this);
    }

    public void addPlayers(Set<Player> players) {
        this.players = players;
        for (Player player : this.players) {
            player.getTeams().add(this);
        }
    }

    public void deletePlayer(Player player) {
        this.players.remove(player);
        player.getTeams().remove(this);
    }
}
