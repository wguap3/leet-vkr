package com.stackr.api.dto.request;

public record UpdateTestCaseRequest(
        String inputData,
        String expectedOutput,
        Boolean isPublic,
        Integer orderIndex
) {}
