package com.stackr.api.controller;

import com.stackr.api.entity.Problem;
import com.stackr.api.entity.TestCase;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.repository.TestCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TestCaseSecurityIntegrationTest extends BaseIntegrationTest {
    @Autowired TestCaseRepository tests;

    @Test
    void unrelatedOrganizerCannotReadHiddenTestsOrChangeThem() throws Exception {
        registerAndLogin("author", "author@example.com", "password123");
        String stranger = registerAndLogin("otherorg", "other@example.com", "password123");
        var author = userRepository.findByUsername("author").orElseThrow();
        author.setRole(Role.ORGANIZER);
        userRepository.save(author);
        var other = userRepository.findByUsername("otherorg").orElseThrow();
        other.setRole(Role.ORGANIZER);
        userRepository.save(other);
        var problem = problemRepository.save(Problem.builder().title("Private tests").description("desc")
                .difficulty(Difficulty.EASY).author(author).visible(true).build());
        var hidden = tests.save(TestCase.builder().problem(problem).inputData("secret")
                .expectedOutput("hidden answer").isPublic(false).build());
        String endpoint = "/api/problems/" + problem.getId() + "/testcases";
        mockMvc.perform(get(endpoint).header("Authorization", bearer(stranger)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mockMvc.perform(post(endpoint).header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputData\":\"1\",\"expectedOutput\":\"1\",\"isPublic\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(endpoint + "/" + hidden.getId()).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/submissions/problem/" + problem.getId()).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
    }
}
