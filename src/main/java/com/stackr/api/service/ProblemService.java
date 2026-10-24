package com.stackr.api.service;

import com.stackr.api.dto.request.CreateProblemRequest;
import com.stackr.api.dto.request.UpdateProblemRequest;
import com.stackr.api.dto.response.ProblemDetailDto;
import com.stackr.api.dto.response.ProblemShortDto;
import com.stackr.api.dto.response.ProblemStatsDto;
import com.stackr.api.dto.response.TagDto;
import com.stackr.api.dto.response.TestCaseDto;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.ProblemRepository;
import com.stackr.api.repository.SubmissionRepository;
import com.stackr.api.repository.TagRepository;
import com.stackr.api.repository.TestCaseRepository;
import com.stackr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public Page<ProblemShortDto> getProblems(Difficulty difficulty, Long tagId, String search, Long userId, Pageable pageable) {
        // Normalize search: blank string → null (no filter)
        String normalizedSearch = (search != null && !search.isBlank()) ? search.strip() : null;
        Page<Problem> page = problemRepository.findByFilters(difficulty, tagId, normalizedSearch, pageable);
        List<Long> ids = page.getContent().stream().map(Problem::getId).toList();

        if (ids.isEmpty()) return page.map(p -> toShortDto(p, 0L, false));

        // Batch fetch solved-by-user counts (one query for all problems)
        Map<Long, Long> solvedCounts = new HashMap<>();
        submissionRepository.countDistinctUsersByProblemIdsAndVerdict(ids, Verdict.AC)
                .forEach(row -> solvedCounts.put((Long) row[0], (Long) row[1]));

        // Batch fetch which problems the current user has solved
        Set<Long> userSolved = userId != null
                ? new HashSet<>(submissionRepository.findSolvedProblemIdsByUserIdAndProblemIds(userId, ids))
                : Set.of();

        return page.map(p -> toShortDto(p,
                solvedCounts.getOrDefault(p.getId(), 0L),
                userSolved.contains(p.getId())));
    }

    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemById(Long id) {
        var problem = findOrThrow(id);
        var publicTests = testCaseRepository.findByProblemIdAndIsPublicTrueOrderByOrderIndexAsc(id)
                .stream().map(this::toTestCaseDto).toList();
        return toDetailDto(problem, publicTests);
    }

    @Transactional
    public ProblemDetailDto createProblem(CreateProblemRequest req, Long authorId) {
        var author = userRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authorId));

        Set<com.stackr.api.entity.Tag> tags = resolveTags(req.tagIds());

        var problem = Problem.builder()
                .title(req.title())
                .description(req.description())
                .difficulty(req.difficulty())
                .timeLimitMs(req.timeLimitMs() != null ? req.timeLimitMs() : 2000)
                .memoryLimitMb(req.memoryLimitMb() != null ? req.memoryLimitMb() : 256)
                .tags(tags)
                .author(author)
                .codeTemplate(req.codeTemplate())
                .build();

        problem = problemRepository.save(problem);
        return toDetailDto(problem, List.of());
    }

    @Transactional
    public ProblemDetailDto updateProblem(Long id, UpdateProblemRequest req, Long userId) {
        var problem = findOrThrow(id);
        checkOwnership(problem, userId, "update");

        if (req.title() != null) problem.setTitle(req.title());
        if (req.description() != null) problem.setDescription(req.description());
        if (req.difficulty() != null) problem.setDifficulty(req.difficulty());
        if (req.timeLimitMs() != null) problem.setTimeLimitMs(req.timeLimitMs());
        if (req.memoryLimitMb() != null) problem.setMemoryLimitMb(req.memoryLimitMb());
        if (req.tagIds() != null) problem.setTags(resolveTags(req.tagIds()));
        if (req.codeTemplate() != null) problem.setCodeTemplate(req.codeTemplate());

        problem = problemRepository.save(problem);
        var publicTests = testCaseRepository.findByProblemIdAndIsPublicTrueOrderByOrderIndexAsc(id)
                .stream().map(this::toTestCaseDto).toList();
        return toDetailDto(problem, publicTests);
    }

    @Transactional
    public void deleteProblem(Long id, Long userId) {
        var problem = findOrThrow(id);
        checkOwnership(problem, userId, "delete");
        problemRepository.deleteById(id);
    }

    @Transactional
    public void setVisibility(Long id, boolean visible, Long userId) {
        var problem = findOrThrow(id);
        checkOwnership(problem, userId, "change visibility of");
        problem.setVisible(visible);
        problemRepository.save(problem);
    }

    @Transactional(readOnly = true)
    public ProblemStatsDto getProblemStats(Long id) {
        findOrThrow(id); // ensure exists
        var rows = submissionRepository.countByProblemIdGroupByVerdict(id);
        long ac = 0, wa = 0, tle = 0, re = 0, ce = 0;
        for (Object[] row : rows) {
            String verdict = row[0].toString();
            long count = ((Number) row[1]).longValue();
            switch (verdict) {
                case "AC"  -> ac  = count;
                case "WA"  -> wa  = count;
                case "TLE" -> tle = count;
                case "RE"  -> re  = count;
                case "CE"  -> ce  = count;
            }
        }
        long total = ac + wa + tle + re + ce;
        Double avgMs = submissionRepository.avgExecutionTimeByProblemId(id);
        double rate = total == 0 ? 0.0 : (ac * 100.0 / total);
        return new ProblemStatsDto(id, total, ac, wa, tle, re, ce, avgMs, rate);
    }

    private void checkOwnership(Problem problem, Long userId, String action) {
        if (!problem.getAuthor().getId().equals(userId)) {
            var caller = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
            if (caller.getRole() != Role.ADMIN) {
                throw new BusinessException("Not authorized to " + action + " this problem");
            }
        }
    }

    private Problem findOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Problem with id=" + id + " not found"));
    }

    private Set<com.stackr.api.entity.Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        return tagIds.stream()
                .map(tid -> tagRepository.findById(tid)
                        .orElseThrow(() -> new EntityNotFoundException("Tag not found: " + tid)))
                .collect(Collectors.toSet());
    }

    private ProblemShortDto toShortDto(Problem p, long solvedCount, boolean userSolved) {
        Verdict userVerdict = userSolved ? Verdict.AC : null;
        Set<TagDto> tags = p.getTags().stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .collect(Collectors.toSet());
        return new ProblemShortDto(p.getId(), p.getTitle(), p.getDifficulty(), tags, solvedCount, userVerdict);
    }

    private ProblemDetailDto toDetailDto(Problem p, List<TestCaseDto> publicTests) {
        Set<TagDto> tags = p.getTags().stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .collect(Collectors.toSet());
        return new ProblemDetailDto(p.getId(), p.getTitle(), p.getDescription(), p.getDifficulty(),
                p.getTimeLimitMs(), p.getMemoryLimitMb(), tags, p.getCodeTemplate(), publicTests);
    }

    private TestCaseDto toTestCaseDto(com.stackr.api.entity.TestCase tc) {
        return new TestCaseDto(tc.getId(), tc.getInputData(), tc.getExpectedOutput(), tc.isPublic(), tc.getOrderIndex());
    }
}
