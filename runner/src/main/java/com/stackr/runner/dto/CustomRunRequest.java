package com.stackr.runner.dto;

public record CustomRunRequest(
        String sourceCode,
        String input,
        int timeLimitMs,
        int memoryLimitMb
) {}
