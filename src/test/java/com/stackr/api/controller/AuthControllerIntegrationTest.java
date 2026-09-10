package com.stackr.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController — integration tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── /api/auth/register ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register — 201: успешная регистрация")
    void register_validData_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("CONTESTANT"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /register — 400: невалидный email")
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"not-an-email","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /register — 400: пароль слишком короткий (менее 8 символов)")
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"bob@example.com","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register — 409: username уже занят")
    void register_duplicateUsername_returns409() throws Exception {
        // Первая регистрация
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        // Повторная с тем же username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice2@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── /api/auth/login ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login — 200: верные данные → JWT токены")
    void login_validCredentials_returns200WithTokens() throws Exception {
        // Сначала регистрируем пользователя
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        // Логин
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /login — 401: неверный пароль")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login — 401: пользователь не существует")
    void login_unknownUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── /api/auth/logout ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /logout — 204: всегда успешен (stateless JWT)")
    void logout_anyRequest_returns204() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }
}
