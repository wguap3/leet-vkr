package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRequest(
        @NotNull Long problemId,
        @NotBlank @Size(max = 65536) String sourceCode,
        Long contestId
) {}
