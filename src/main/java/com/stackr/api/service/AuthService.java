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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }
        // Only CONTESTANT and ORGANIZER can be self-assigned; ADMIN is DB-only
        Role role = "ORGANIZER".equalsIgnoreCase(req.role()) ? Role.ORGANIZER : Role.CONTESTANT;

        var user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .build();
        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        var user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return buildTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return buildTokenResponse(user);
    }

    private TokenResponse buildTokenResponse(User user) {
        String access = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getId());
        return new TokenResponse(access, refresh, jwtUtil.getAccessTtlSeconds());
    }

    private UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getRating(), u.getRegisteredAt());
    }
}
