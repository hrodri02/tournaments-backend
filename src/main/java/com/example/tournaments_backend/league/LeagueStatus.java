package com.example.tournaments_backend.league;

public enum LeagueStatus {
    NOT_STARTED("notStarted"),
    IN_PROGRESS("inProgress"),
    ENDED("ended");

    private String displayString;

    private LeagueStatus(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return this.displayString;
    }
}
