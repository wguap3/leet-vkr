package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RunCodeRequest(
        @NotNull Long problemId,
        @NotBlank @Size(max = 65536) String sourceCode,
        @Size(max = 1048576) String customInput   // nullable — empty string means no stdin
) {}
