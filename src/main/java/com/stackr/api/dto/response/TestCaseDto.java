package com.stackr.api.dto.response;

public record TestCaseDto(
        Long id,
        String inputData,
        String expectedOutput,
        boolean isPublic,
        Integer orderIndex
) {}
