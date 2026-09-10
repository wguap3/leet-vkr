package com.stackr.api.dto.response;

public record LeaderboardEntryDto(
        int rank,
        Long userId,
        String username,
        Integer score,
        Integer penalty,
        Integer submissionCount
) {}
