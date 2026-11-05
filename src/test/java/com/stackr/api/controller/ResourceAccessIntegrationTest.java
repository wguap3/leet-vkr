package com.stackr.api.controller;

import com.stackr.api.entity.Contest;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceAccessIntegrationTest extends BaseIntegrationTest {
    @Test
    void unrelatedOrganizerCannotReadDraftOrPrivateContestById() throws Exception {
        registerAndLogin("resourceauthor", "resourceauthor@example.com", "password123");
        String otherToken = registerAndLogin("resourceother", "resourceother@example.com", "password123");
        var owner = userRepository.findByUsername("resourceauthor").orElseThrow();
        owner.setRole(Role.ORGANIZER);
        userRepository.save(owner);
        var other = userRepository.findByUsername("resourceother").orElseThrow();
        other.setRole(Role.ORGANIZER);
        userRepository.save(other);
        var draft = problemRepository.save(Problem.builder().title("Draft").description("secret")
                .difficulty(Difficulty.EASY).author(owner).visible(false).build());
        var contest = contestRepository.save(Contest.builder().title("Private").organizer(owner)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(2))
                .isPublic(false).build());
        mockMvc.perform(get("/api/problems/" + draft.getId()).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        for (String suffix : new String[]{"", "/standings", "/analytics", "/submissions"}) {
            mockMvc.perform(get("/api/contests/" + contest.getId() + suffix).header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());
        }
    }
}
