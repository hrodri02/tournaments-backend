package com.example.tournaments_backend.player;

import java.util.Set;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.game_stat.GameStat;
import com.example.tournaments_backend.team.Team;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "team_id")
    private Team team;
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
