package com.stackr.api.dto.request;

import com.stackr.api.entity.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record CreateProblemRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull Difficulty difficulty,
        @Positive Integer timeLimitMs,
        @Positive Integer memoryLimitMb,
        Set<Long> tagIds,
        String codeTemplate
) {}
