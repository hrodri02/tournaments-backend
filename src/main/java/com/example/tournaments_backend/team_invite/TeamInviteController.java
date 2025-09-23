package com.example.tournaments_backend.team_invite;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path="/api/v1/teaminvites")
@Tag(name = "Team Invites Management", description = "API endpoints for managing team invites")
public class TeamInviteController {
    private final TeamInviteService teamInviteService;

    @Autowired
    public TeamInviteController(TeamInviteService teamInviteService) {
        this.teamInviteService = teamInviteService;
    }
}
