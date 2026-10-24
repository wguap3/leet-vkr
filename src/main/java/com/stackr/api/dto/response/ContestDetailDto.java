package com.stackr.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stackr.api.entity.enums.ContestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ContestDetailDto(
        Long id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ContestStatus status,
        List<ProblemShortDto> problems,
        @JsonProperty("isRegistered") boolean isRegistered,
        @JsonProperty("isPublic") boolean isPublic,
        String inviteToken,
        Long organizerId
) {}
