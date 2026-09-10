package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.ContestStatus;

import java.time.LocalDateTime;

public record ContestShortDto(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ContestStatus status,
        int problemCount,
        long participantCount
) {}
