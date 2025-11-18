package com.example.tournaments_backend.league_application;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path="/api/v1/applications")
@Tag(name = "League Application Management", description = "API endpoints for managing league applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
}