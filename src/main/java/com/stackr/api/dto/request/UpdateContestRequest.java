package com.stackr.api.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateContestRequest(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Long> problemIds,
        Boolean isPublic
) {}
