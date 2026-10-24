package com.stackr.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * ICPC-style standings for a contest.
 * Penalty = sum of (firstAcMinutes + 20 * wrongAttemptsBeforeAc) for each solved problem.
 */
public record ContestStandingsDto(
        Long contestId,
        String contestTitle,
        List<ProblemHeader> problems,
        List<StandingsRow> rows
) {

    public record ProblemHeader(Long id, String title, String letter) {}

    public record StandingsRow(
            int rank,
            Long userId,
            String username,
            int solved,
            int penalty,
            Map<Long, ProblemCell> cells   // problemId → cell
    ) {}

    /**
     * @param solved       true if the user got AC on this problem
     * @param wrongBefore  wrong submissions before first AC (0 if not solved and no wrong)
     * @param solvedAt     minutes from contest start to first AC (null if not solved)
     */
    public record ProblemCell(
            boolean solved,
            int wrongBefore,
            Integer solvedAt
    ) {}
}
