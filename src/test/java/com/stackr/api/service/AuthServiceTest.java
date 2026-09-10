package com.stackr.api.service;

import com.stackr.api.dto.request.LoginRequest;
import com.stackr.api.dto.request.RegisterRequest;
import com.stackr.api.dto.response.TokenResponse;
import com.stackr.api.dto.response.UserDto;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.exception.ConflictException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.UserRepository;
import com.stackr.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — unit tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("$2a$hashed")
                .role(Role.CONTESTANT)
                .rating(0)
                .registeredAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: успешная регистрация возвращает UserDto с ролью CONTESTANT")
    void register_success_returnsUserDto() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.existsByEmail("alice@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$hashed");
        given(userRepository.save(any())).willReturn(alice);

        UserDto result = authService.register(
                new RegisterRequest("alice", "alice@example.com", "password123"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.role()).isEqualTo(Role.CONTESTANT);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("register: дублирующийся username → ConflictException")
    void register_duplicateUsername_throwsConflict() {
        given(userRepository.existsByUsername("alice")).willReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "password123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice");
    }

    @Test
    @DisplayName("register: дублирующийся email → ConflictException")
    void register_duplicateEmail_throwsConflict() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.existsByEmail("alice@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "password123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice@example.com");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: корректные данные → TokenResponse с токенами")
    void login_validCredentials_returnsTokenResponse() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(alice));
        given(passwordEncoder.matches("password123", "$2a$hashed")).willReturn(true);
        given(jwtUtil.generateAccessToken(1L, "alice")).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(1L)).willReturn("refresh-token");
        given(jwtUtil.getAccessTtlSeconds()).willReturn(86400L);

        TokenResponse result = authService.login(new LoginRequest("alice", "password123"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("login: неверный пароль → BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(alice));
        given(passwordEncoder.matches("wrong", "$2a$hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: пользователь не найден → BadCredentialsException")
    void login_unknownUser_throwsBadCredentials() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh: валидный токен → новый TokenResponse")
    void refresh_validToken_returnsNewTokenResponse() {
        given(jwtUtil.validateToken("valid-refresh")).willReturn(true);
        given(jwtUtil.getUserIdFromToken("valid-refresh")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(jwtUtil.generateAccessToken(1L, "alice")).willReturn("new-access");
        given(jwtUtil.generateRefreshToken(1L)).willReturn("new-refresh");
        given(jwtUtil.getAccessTtlSeconds()).willReturn(86400L);

        TokenResponse result = authService.refresh("valid-refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
    }

    @Test
    @DisplayName("refresh: невалидный токен → BadCredentialsException")
    void refresh_invalidToken_throwsBadCredentials() {
        given(jwtUtil.validateToken("bad-token")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refresh: токен валиден, но пользователь удалён → EntityNotFoundException")
    void refresh_userNotFound_throwsEntityNotFoundException() {
        given(jwtUtil.validateToken("orphan-token")).willReturn(true);
        given(jwtUtil.getUserIdFromToken("orphan-token")).willReturn(99L);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("orphan-token"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
