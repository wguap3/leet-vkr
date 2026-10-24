package com.stackr.api.dto.response;

public record HintDto(
        Long id,
        String text,
        int orderIndex,
        int unlocksAfterAttempts,
        boolean unlocked   // computed per-user: true if user has enough wrong attempts
) {}
