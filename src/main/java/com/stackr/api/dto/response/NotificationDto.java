package com.stackr.api.dto.response;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt
) {}
