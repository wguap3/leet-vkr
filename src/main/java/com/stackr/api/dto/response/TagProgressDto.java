package com.stackr.api.dto.response;

public record TagProgressDto(
        Long tagId,
        String tagName,
        long solved,
        long total
) {}
