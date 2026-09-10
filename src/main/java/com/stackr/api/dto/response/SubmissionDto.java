package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Verdict;

import java.time.LocalDateTime;

public record SubmissionDto(
        Long id,
        Long userId,
        String username,
        Long problemId,
        String problemTitle,
        Verdict verdict,
        Integer executionTimeMs,
        Integer memoryUsedMb,
        LocalDateTime submittedAt,
        String sourceCode   // null if not owner/ORGANIZER+
) {}
