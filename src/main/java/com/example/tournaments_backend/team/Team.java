package com.example.tournaments_backend.team;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@EqualsAndHashCode
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
    private Long id;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "Team name must be between 2 and 255 characters.")
    private String name;

    public Team(String name) {
        this.name = name;
    }
}
