package com.stackr.api.controller;

import com.stackr.api.entity.Problem;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ProblemController — integration tests (security + business)")
class ProblemControllerIntegrationTest extends BaseIntegrationTest {

    // ── GET /api/problems ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /problems — 401: без авторизации")
    void getProblems_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /problems — 200: авторизованный пользователь получает список")
    void getProblems_authenticated_returns200() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "password123");

        mockMvc.perform(get("/api/problems")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── POST /api/problems ────────────────────────────────────────────────────

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] POST /problems — 403: CONTESTANT не может создавать задачи")
    void createProblem_asContestant_returns403() throws Exception {
        // Contestant — роль по умолчанию при регистрации
        String token = registerAndLogin("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/problems")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Test Problem",
                                  "description":"Some description",
                                  "difficulty":"EASY",
                                  "timeLimitMs":2000,
                                  "memoryLimitMb":256
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /problems — 201: ORGANIZER создаёт задачу")
    void createProblem_asOrganizer_returns201() throws Exception {
        // Создаём пользователя и вручную ставим ему роль ORGANIZER
        String token = registerAndLogin("org", "org@example.com", "password123");
        User organizer = userRepository.findByUsername("org").orElseThrow();
        organizer.setRole(Role.ORGANIZER);
        userRepository.save(organizer);

        // Повторный логин чтобы получить токен с обновлённой ролью
        String orgToken = loginOnly("org", "password123");

        mockMvc.perform(post("/api/problems")
                        .header("Authorization", bearer(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Two Sum",
                                  "description":"Given an array of integers...",
                                  "difficulty":"EASY",
                                  "timeLimitMs":2000,
                                  "memoryLimitMb":256,
                                  "tagIds":[]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.difficulty").value("EASY"));
    }

    @Test
    @DisplayName("GET /problems/{id} — 404: задача не найдена")
    void getProblem_notFound_returns404() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "password123");

        mockMvc.perform(get("/api/problems/99999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] DELETE /problems/{id} — 403: ORGANIZER не может удалять (только ADMIN)")
    void deleteProblem_asOrganizer_returns403() throws Exception {
        // Создаём organizer
        String token = registerAndLogin("org", "org@example.com", "password123");
        User organizer = userRepository.findByUsername("org").orElseThrow();
        organizer.setRole(Role.ORGANIZER);
        userRepository.save(organizer);
        String orgToken = loginOnly("org", "password123");

        // Создаём задачу прямо в БД
        Problem problem = problemRepository.save(Problem.builder()
                .title("Test").description("desc").difficulty(Difficulty.EASY)
                .author(organizer).tags(new HashSet<>()).build());

        mockMvc.perform(delete("/api/problems/" + problem.getId())
                        .header("Authorization", bearer(orgToken)))
                .andExpect(status().isForbidden());
    }

    // ── вспомогательный логин без регистрации ─────────────────────────────────

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
