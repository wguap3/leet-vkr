package com.stackr.runner.dto;

import java.util.List;

public record RunRequest(
        Long submissionId,
        String sourceCode,
        List<TestCaseInput> testCases,
        int timeLimitMs,
        int memoryLimitMb
) {}
