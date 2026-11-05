package com.stackr.runner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RunRequest(
        @NotNull Long submissionId,
        @NotBlank @Size(max = 65536) String sourceCode,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid TestCaseInput> testCases,
        @Min(100) @Max(30000) int timeLimitMs,
        @Min(64) @Max(1024) int memoryLimitMb
) {}
