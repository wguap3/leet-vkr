package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Role;

import java.time.LocalDateTime;

public record UserProfileDto(
        Long id,
        String username,
        String email,
        Role role,
        Integer rating,
        String avatarUrl,
        LocalDateTime registeredAt,
        long totalAC,
        long totalSubmissions
) {}
