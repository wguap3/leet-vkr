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
        String sourceCode, // owner, problem author or administrator
        String errorMessage // compilation diagnostics only; no hidden-test output
) {}
