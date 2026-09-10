package com.stackr.api.controller;

import com.stackr.api.entity.Contest;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SubmissionController — integration tests (security + business)")
class SubmissionControllerIntegrationTest extends BaseIntegrationTest {

    private User organizer;
    private Problem problem;
    private String contestantToken;
    private String organizerToken;

    @BeforeEach
    void setUpData() throws Exception {
        // Регистрируем participant
        contestantToken = registerAndLogin("alice", "alice@example.com", "password123");

        // Регистрируем organizer (меняем роль вручную)
        registerAndLogin("org", "org@example.com", "password123");
        organizer = userRepository.findByUsername("org").orElseThrow();
        organizer.setRole(Role.ORGANIZER);
        userRepository.save(organizer);
        organizerToken = loginOnly("org", "password123");

        // Создаём задачу прямо в БД
        problem = problemRepository.save(Problem.builder()
                .title("Two Sum").description("Find two numbers")
                .difficulty(Difficulty.EASY).timeLimitMs(2000).memoryLimitMb(256)
                .author(organizer).tags(new HashSet<>()).visible(true)
                .build());
    }

    // ── POST /api/submissions ─────────────────────────────────────────────────

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] POST /submissions — 403: ORGANIZER не может делать посылки")
    void submit_asOrganizer_returns403() throws Exception {
        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemId":%d,"sourceCode":"class Solution {}"}
                                """.formatted(problem.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /submissions — 401: без авторизации")
    void submit_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemId":1,"sourceCode":"class Solution {}"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /submissions — 202: CONTESTANT успешно отправляет посылку")
    void submit_asContestant_returns202WithPending() throws Exception {
        // Runner замокан — ничего не делает
        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", bearer(contestantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemId":%d,"sourceCode":"class Solution {}"}
                                """.formatted(problem.getId())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verdict").value("PENDING"))
                .andExpect(jsonPath("$.problemId").value(problem.getId()));
    }

    @Test
    @DisplayName("[БИЗНЕС-ЛОГИКА] POST /submissions — 422: посылка в завершённый контест")
    void submit_toFinishedContest_returns422() throws Exception {
        // Создаём завершённый контест
        Contest finishedContest = contestRepository.save(Contest.builder()
                .title("Old Contest")
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .status(ContestStatus.FINISHED)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", bearer(contestantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemId":%d,
                                  "sourceCode":"class Solution {}",
                                  "contestId":%d
                                }
                                """.formatted(problem.getId(), finishedContest.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST /submissions — 400: пустой sourceCode")
    void submit_emptySourceCode_returns400() throws Exception {
        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", bearer(contestantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"problemId":%d,"sourceCode":""}
                                """.formatted(problem.getId())))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/submissions/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /submissions/{id} — 404: посылка не найдена")
    void getSubmission_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/submissions/99999")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/submissions/problem/{id} ─────────────────────────────────────

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] GET /submissions/problem/{id} — 403: CONTESTANT не имеет доступа")
    void getSubmissionsByProblem_asContestant_returns403() throws Exception {
        mockMvc.perform(get("/api/submissions/problem/" + problem.getId())
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /submissions/problem/{id} — 200: ORGANIZER видит все посылки по задаче")
    void getSubmissionsByProblem_asOrganizer_returns200() throws Exception {
        mockMvc.perform(get("/api/submissions/problem/" + problem.getId())
                        .header("Authorization", bearer(organizerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ─ вспомогательный метод ──────────────────────────────────────────────────

    private String loginOnly(String username, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
