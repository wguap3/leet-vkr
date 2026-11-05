package com.stackr.api.dto.request;

import com.stackr.api.entity.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

public record CreateProblemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull Difficulty difficulty,
        @Min(100) @Max(30000) Integer timeLimitMs,
        @Min(64) @Max(1024) Integer memoryLimitMb,
        Set<Long> tagIds,
        String codeTemplate
) {}
