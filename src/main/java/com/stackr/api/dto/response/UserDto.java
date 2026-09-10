package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role,
        Integer rating,
        LocalDateTime registeredAt
) {}
