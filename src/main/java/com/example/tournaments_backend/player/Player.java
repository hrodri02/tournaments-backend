package com.example.tournaments_backend.player;

import java.util.HashSet;
import java.util.Set;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.game_stat.GameStat;
import com.example.tournaments_backend.team.Team;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@PrimaryKeyJoinColumn(name = "player_id") 
@Getter
@Setter
@NoArgsConstructor
public class Player extends AppUser {
    @Enumerated(EnumType.STRING)
    private Position position;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "team_player",
        joinColumns = @JoinColumn(name = "player_id"),
        inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    @JsonIgnore
    private Set<Team> teams = new HashSet<>();
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<GameStat> gameStats;

    public Player(String firstName,
                  String lastName, 
                  String email, 
                  String password,
                  AppUserRole appUserRole,
                  Position position)
    {
        super(firstName, lastName, email, password, appUserRole);
        this.position = position;
    }
}
