package com.stackr.api.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record CreateTestCaseRequest(
        @NotNull @Size(max = 1048576) String inputData,
        @NotNull @Size(max = 1048576) String expectedOutput,
        @NotNull Boolean isPublic,
        Integer orderIndex
) {}
