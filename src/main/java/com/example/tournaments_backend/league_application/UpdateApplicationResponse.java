package com.example.tournaments_backend.league_application;

import com.example.tournaments_backend.league.LeagueDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateApplicationResponse {
    ApplicationDTO updatedApplication;
    LeagueDTO updatedLeague;
}
