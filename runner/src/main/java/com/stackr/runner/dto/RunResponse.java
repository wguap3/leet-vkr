package com.stackr.runner.dto;

import lombok.Builder;

@Builder
public record RunResponse(
        Long submissionId,
        String verdict,
        Integer executionTimeMs,
        Integer memoryUsedMb,
        String errorMessage
) {}
