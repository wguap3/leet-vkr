package com.stackr.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackr.api.dto.request.LoginRequest;
import com.stackr.api.dto.request.RegisterRequest;
import com.stackr.api.repository.*;
import com.stackr.api.service.RunnerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Базовый класс для интеграционных тестов.
 * БД — H2 in-memory (src/test/resources/application.yml).
 * RunnerClient замокан — Docker не нужен.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbcTemplate;

    @Autowired protected SubmissionRepository submissionRepository;
    @Autowired protected ContestParticipationRepository participationRepository;
    @Autowired protected ContestRepository contestRepository;
    @Autowired protected ProblemRepository problemRepository;
    @Autowired protected TagRepository tagRepository;
    @Autowired protected UserRepository userRepository;

    // Runner не нужен — подменяем моком чтобы фоновый поток не падал с NPE
    @MockBean protected RunnerClient runnerClient;

    @BeforeEach
    void configureRunnerMock() {
        var mockResponse = new RunnerClient.RunnerResponse();
        mockResponse.setVerdict("AC");
        mockResponse.setExecutionTimeMs(120);
        mockResponse.setMemoryUsedMb(32);
        given(runnerClient.run(anyLong(), anyString(), any(), anyInt(), anyInt()))
                .willReturn(mockResponse);
    }

    /**
     * Очищаем все таблицы в правильном порядке (с учётом FK).
     * Используем JdbcTemplate для прямой очистки join-таблиц.
     */
    @AfterEach
    void cleanDatabase() {
        submissionRepository.deleteAllInBatch();
        participationRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM contest_problems");
        contestRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM problem_tags");
        problemRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    /**
     * Регистрирует пользователя и возвращает JWT access token.
     */
    protected String registerAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, password, null))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
