package com.stackr.api.dto.response;

public record RunCodeResult(
        String stdout,
        String stderr,
        int elapsedMs,
        String status   // "OK" | "CE" | "TLE" | "RE"
) {}
