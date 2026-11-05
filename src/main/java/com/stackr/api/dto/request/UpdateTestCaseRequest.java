package com.stackr.api.dto.request;
import jakarta.validation.constraints.Size;

public record UpdateTestCaseRequest(
        @Size(max = 1048576) String inputData,
        @Size(max = 1048576) String expectedOutput,
        Boolean isPublic,
        Integer orderIndex
) {}
