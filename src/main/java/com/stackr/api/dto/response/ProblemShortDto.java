package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Verdict;

import java.util.Set;

public record ProblemShortDto(
        Long id,
        String title,
        Difficulty difficulty,
        Set<TagDto> tags,
        long solvedCount,
        Verdict userVerdict,
        Long authorId,
        boolean visible
) {
    public ProblemShortDto(Long id, String title, Difficulty difficulty, Set<TagDto> tags, long solvedCount, Verdict userVerdict) {
        this(id, title, difficulty, tags, solvedCount, userVerdict, null, true);
    }
}
