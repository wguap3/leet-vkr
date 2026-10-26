package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Difficulty;

import java.util.List;
import java.util.Set;

public record ProblemDetailDto(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Set<TagDto> tags,
        String codeTemplate,
        List<TestCaseDto> publicTestCases,
        boolean visible
) {}
