package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunCodeRequest(
        @NotNull Long problemId,
        @NotBlank String sourceCode,
        String customInput   // nullable — empty string means no stdin
) {}
