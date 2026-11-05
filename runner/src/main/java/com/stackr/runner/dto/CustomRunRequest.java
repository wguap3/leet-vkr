package com.stackr.runner.dto;
import jakarta.validation.constraints.*;

public record CustomRunRequest(
        @NotBlank @Size(max = 65536) String sourceCode,
        @Size(max = 1048576) String input,
        @Min(100) @Max(30000) int timeLimitMs,
        @Min(64) @Max(1024) int memoryLimitMb
) {}
