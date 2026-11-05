package com.stackr.api.controller;

import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.service.SubmissionProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubmissionQueueIntegrationTest extends BaseIntegrationTest {
    @Autowired SubmissionProcessor processor;

    @Test
    void committedPendingSubmissionIsProcessedOnceFromDatabase() throws Exception {
        String token = registerAndLogin("queueuser", "queue@example.com", "password123");
        var user = userRepository.findByUsername("queueuser").orElseThrow();
        var problem = problemRepository.save(Problem.builder().author(user).title("Sum")
                .description("Sum two integers").difficulty(Difficulty.EASY).visible(true).build());
        var result = mockMvc.perform(post("/api/submissions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":" + problem.getId() + ",\"sourceCode\":\"class Solution {}\"}"))
                .andExpect(status().isAccepted()).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(submissionRepository.findById(id).orElseThrow().getVerdict()).isEqualTo(Verdict.PENDING);
        // New transaction discovers the persisted job, as it would after an API restart.
        processor.processNext();
        assertThat(submissionRepository.findById(id).orElseThrow().getVerdict()).isEqualTo(Verdict.AC);
        int rating = userRepository.findById(user.getId()).orElseThrow().getRating();
        processor.processNext();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRating()).isEqualTo(rating);
    }
}
