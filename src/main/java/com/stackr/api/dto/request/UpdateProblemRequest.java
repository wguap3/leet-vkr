package com.stackr.api.dto.request;

import com.stackr.api.entity.enums.Difficulty;

import java.util.Set;

public record UpdateProblemRequest(
        String title,
        String description,
        Difficulty difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Set<Long> tagIds,
        String codeTemplate
) {}
