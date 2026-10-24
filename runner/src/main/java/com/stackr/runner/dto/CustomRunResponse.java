package com.stackr.runner.dto;

public record CustomRunResponse(
        String stdout,
        String stderr,
        int exitCode,
        int elapsedMs,
        String status   // "OK" | "CE" | "TLE" | "RE"
) {}
