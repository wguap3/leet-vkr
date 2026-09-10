package com.stackr.api.controller;

import com.stackr.api.entity.Contest;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ContestController — integration tests (security + business)")
class ContestControllerIntegrationTest extends BaseIntegrationTest {

    private User organizer;
    private String contestantToken;
    private String organizerToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        contestantToken = registerAndLogin("alice", "alice@example.com", "password123");

        registerAndLogin("org", "org@example.com", "password123");
        organizer = userRepository.findByUsername("org").orElseThrow();
        organizer.setRole(Role.ORGANIZER);
        userRepository.save(organizer);
        organizerToken = loginOnly("org", "password123");
    }

    // ── GET /api/contests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /contests — 200: список контестов для авторизованного")
    void getContests_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/contests")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /contests — 401: без авторизации")
    void getContests_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/contests"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/contests ────────────────────────────────────────────────────

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] POST /contests — 403: CONTESTANT не может создавать контесты")
    void createContest_asContestant_returns403() throws Exception {
        mockMvc.perform(post("/api/contests")
                        .header("Authorization", bearer(contestantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"My Contest",
                                  "description":"desc",
                                  "startTime":"2030-01-01T10:00:00",
                                  "endTime":"2030-01-01T12:00:00",
                                  "problemIds":[],
                                  "isPublic":true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /contests — 201: ORGANIZER создаёт контест")
    void createContest_asOrganizer_returns201() throws Exception {
        mockMvc.perform(post("/api/contests")
                        .header("Authorization", bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Spring Championship",
                                  "description":"Annual contest",
                                  "startTime":"2030-06-01T10:00:00",
                                  "endTime":"2030-06-01T14:00:00",
                                  "problemIds":[],
                                  "isPublic":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Championship"));
    }

    // ── POST /api/contests/{id}/register ──────────────────────────────────────

    @Test
    @DisplayName("POST /contests/{id}/register — 200: CONTESTANT регистрируется на RUNNING контест")
    void registerForContest_running_returns200() throws Exception {
        Contest contest = contestRepository.save(Contest.builder()
                .title("Active Contest")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(ContestStatus.RUNNING)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        mockMvc.perform(post("/api/contests/" + contest.getId() + "/register")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /contests/{id}/register — 409: повторная регистрация")
    void registerForContest_twice_returns409() throws Exception {
        Contest contest = contestRepository.save(Contest.builder()
                .title("Active Contest")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(ContestStatus.RUNNING)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        // Первая регистрация
        mockMvc.perform(post("/api/contests/" + contest.getId() + "/register")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isOk());

        // Повторная → 409
        mockMvc.perform(post("/api/contests/" + contest.getId() + "/register")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("[БИЗНЕС-ЛОГИКА] POST /contests/{id}/register — 422: контест завершён")
    void registerForContest_finished_returns422() throws Exception {
        Contest finishedContest = contestRepository.save(Contest.builder()
                .title("Old Contest")
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .status(ContestStatus.FINISHED)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        mockMvc.perform(post("/api/contests/" + finishedContest.getId() + "/register")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("[БЕЗОПАСНОСТЬ] POST /contests/{id}/register — 403: ORGANIZER не может регистрироваться")
    void registerForContest_asOrganizer_returns403() throws Exception {
        Contest contest = contestRepository.save(Contest.builder()
                .title("Contest")
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(ContestStatus.RUNNING)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        mockMvc.perform(post("/api/contests/" + contest.getId() + "/register")
                        .header("Authorization", bearer(organizerToken)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/contests/{id}/leaderboard ────────────────────────────────────

    @Test
    @DisplayName("GET /contests/{id}/leaderboard — 200: возвращает список лидеров")
    void getLeaderboard_returns200() throws Exception {
        Contest contest = contestRepository.save(Contest.builder()
                .title("Contest")
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(ContestStatus.RUNNING)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .isPublic(true)
                .build());

        mockMvc.perform(get("/api/contests/" + contest.getId() + "/leaderboard")
                        .header("Authorization", bearer(contestantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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
