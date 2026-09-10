package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitRequest(
        @NotNull Long problemId,
        @NotBlank String sourceCode,
        Long contestId
) {}
