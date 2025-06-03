package com.example.tournaments_backend.league;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Period;
import java.util.Date;

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
    private String name;
    @Column(name = "start_date")
    private Date startDate;
    private Period duration;

    public League(String name, Date startDate, Period duration) {
        this.name = name;
        this.startDate = startDate;
        this.duration = duration;
    }
}
