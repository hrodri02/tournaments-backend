package com.example.tournaments_backend.league_application;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.tournaments_backend.team.TeamDTO;
import com.example.tournaments_backend.league.LeagueDTO;

@Getter
@NoArgsConstructor
public class ApplicationDTO {
    private Long id;
    private TeamDTO team;
    private LeagueDTO league;
    private ApplicationStatus status;
    private LocalDateTime createdAt;

    public ApplicationDTO(Application application) {
        this.id = application.getId();
        this.team = new TeamDTO(application.getTeam());
        this.league = new LeagueDTO(application.getLeague());
        this.status = application.getStatus();
        this.createdAt = application.getCreatedAt();
    }

    public static List<ApplicationDTO> convert(List<Application> applications) {
        if (applications == null || applications.size() == 0) return List.of();

        return applications
                .stream()
                .map(ApplicationDTO::new)
                .toList();
    }
}
