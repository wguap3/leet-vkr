package com.stackr.api.dto.response;

import java.util.Map;

public record ProblemStatsDto(
        Long problemId,
        long totalSubmissions,
        long acCount,
        long waCount,
        long tleCount,
        long reCount,
        long ceCount,
        Double avgExecutionTimeMs,
        double acceptanceRate   // 0..100
) {}
