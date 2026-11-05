package com.stackr.api.controller;

import com.stackr.api.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerIntegrationTest extends BaseIntegrationTest {
    @Test void blockedUserCannotLoginRefreshOrUseExistingAccessToken() throws Exception {
        String token = registerAndLogin("blocked", "blocked@example.com", "password123");
        var login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"blocked\",\"password\":\"password123\"}")).andReturn();
        String refresh = objectMapper.readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();
        var user = userRepository.findByUsername("blocked").orElseThrow(); user.setActive(false); userRepository.save(user);
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"blocked\",\"password\":\"password123\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN).content(refresh)).andExpect(status().isUnauthorized());
    }
    @Test void adminListIncludesRealActiveFlagAndProtectsSelf() throws Exception {
        String token = registerAndLogin("adminqa", "adminqa@example.com", "password123");
        var user = userRepository.findByUsername("adminqa").orElseThrow(); user.setRole(Role.ADMIN); userRepository.save(user);
        mockMvc.perform(get("/api/users?search=adminqa").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].active").value(true));
        mockMvc.perform(patch("/api/users/" + user.getId() + "/active?active=false").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/users/" + user.getId() + "/role?role=CONTESTANT").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }
    @Test void publicProfileDoesNotExposeEmail() throws Exception {
        String token = registerAndLogin("viewer", "viewer@example.com", "password123");
        registerAndLogin("publicuser", "private@example.com", "password123");
        long id = userRepository.findByUsername("publicuser").orElseThrow().getId();
        mockMvc.perform(get("/api/users/"+id).header("Authorization", bearer(token))).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
    }
}
