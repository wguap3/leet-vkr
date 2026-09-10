package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateContestRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        List<Long> problemIds,
        Boolean isPublic
) {}
