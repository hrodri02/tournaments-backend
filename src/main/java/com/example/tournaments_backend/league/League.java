package com.example.tournaments_backend.league;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class League {
    @Id
    @SequenceGenerator(
        name="league_sequence",
        sequenceName="league_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "league_sequence"
    )
    private Long id;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "League name must be between 2 and 255 characters.")
    private String name;
    @NotNull
    @Future
    @Column(name = "start_date")
    private LocalDate startDate;
    @NotNull
    @Min(value = 4, message = "League duration must be at least 4 weeks long.")
    @Column(name = "duration_in_weeks")
    private Integer durationInWeeks;

    public League(String name, LocalDate startDate, Integer durationInWeeks) {
        this.name = name;
        this.startDate = startDate;
        this.durationInWeeks = durationInWeeks;
    }
}
