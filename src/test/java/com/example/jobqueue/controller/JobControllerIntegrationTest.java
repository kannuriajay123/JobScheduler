package com.example.jobqueue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitJobReturnsJobIdAndJobProcessesInBackground() throws Exception {
        String requestBody = "{\"payload\":{\"message\":\"integration-test\"}}";

        String response = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = new ObjectMapper().readTree(response).get("jobId").asText();

        await().atMost(ofSeconds(10)).untilAsserted(() ->
                mockMvc.perform(get("/api/jobs/" + jobId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
        );
    }

    @Test
    void getJobStatusReturnsNotFoundForUnknownJobId() throws Exception {
        mockMvc.perform(get("/api/jobs/unknown-job-id-123"))
                .andExpect(status().isNotFound());
    }
}
