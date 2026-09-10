package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTestCaseRequest(
        @NotBlank String inputData,
        @NotBlank String expectedOutput,
        @NotNull Boolean isPublic,
        Integer orderIndex
) {}
