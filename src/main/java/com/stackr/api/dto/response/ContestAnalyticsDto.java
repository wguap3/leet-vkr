package com.stackr.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ContestAnalyticsDto(
        Long contestId,
        String contestTitle,
        List<ProblemHeader> problems,
        List<ParticipantRow> participants
) {
    public record ProblemHeader(Long id, String title) {}

    public record ParticipantRow(
            Long userId,
            String username,
            Map<Long, ProblemResult> results   // problemId → result
    ) {}

    public record ProblemResult(
            int attempts,     // total submissions on this problem in this contest
            boolean solved,   // has AC
            Integer firstAcMinutes  // minutes from contest start to first AC (null if not solved)
    ) {}
}
