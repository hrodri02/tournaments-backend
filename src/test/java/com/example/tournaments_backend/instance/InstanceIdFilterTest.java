package com.example.tournaments_backend.instance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class InstanceIdFilterTest {

    @RestController
    static class PingController {
        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }

    private MockMvc mockMvcWith(InstanceIdFilter filter) {
        return MockMvcBuilders.standaloneSetup(new PingController())
                .addFilter(filter)
                .build();
    }

    @Test
    void setsConfiguredInstanceIdHeader() throws Exception {
        MockMvc mockMvc = mockMvcWith(new InstanceIdFilter("backend-1"));

        mockMvc.perform(get("/ping"))
                .andExpect(header().string("X-Instance-Id", "backend-1"));
    }

    @Test
    void fallsBackToDefaultInstanceId_whenNotConfigured() throws Exception {
        MockMvc mockMvc = mockMvcWith(new InstanceIdFilter(InstanceIdFilter.DEFAULT_INSTANCE_ID));

        mockMvc.perform(get("/ping"))
                .andExpect(header().string("X-Instance-Id", InstanceIdFilter.DEFAULT_INSTANCE_ID));
    }
}
