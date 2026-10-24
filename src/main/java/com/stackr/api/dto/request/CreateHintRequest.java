package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateHintRequest(
        @NotBlank String text,
        int orderIndex,
        int unlocksAfterAttempts
) {}
