package com.stackr.api.service;

import com.stackr.api.dto.request.CreateProblemRequest;
import com.stackr.api.dto.request.UpdateProblemRequest;
import com.stackr.api.dto.response.ProblemDetailDto;
import com.stackr.api.dto.response.ProblemShortDto;
import com.stackr.api.dto.response.TagDto;
import com.stackr.api.dto.response.TestCaseDto;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Verdict;
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

import java.util.HashSet;
import java.util.List;
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
    public Page<ProblemShortDto> getProblems(Difficulty difficulty, Long tagId, Long userId, Pageable pageable) {
        return problemRepository.findByFilters(difficulty, tagId, pageable)
                .map(p -> toShortDto(p, userId));
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
    public ProblemDetailDto updateProblem(Long id, UpdateProblemRequest req) {
        var problem = findOrThrow(id);

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
    public void deleteProblem(Long id) {
        if (!problemRepository.existsById(id)) {
            throw new EntityNotFoundException("Problem not found: " + id);
        }
        problemRepository.deleteById(id);
    }

    @Transactional
    public void setVisibility(Long id, boolean visible) {
        var problem = findOrThrow(id);
        problem.setVisible(visible);
        problemRepository.save(problem);
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

    private ProblemShortDto toShortDto(Problem p, Long userId) {
        long solved = submissionRepository.countDistinctUsersByProblemIdAndVerdict(p.getId(), Verdict.AC);
        Verdict userVerdict = null;
        if (userId != null && submissionRepository.existsByUserIdAndProblemIdAndVerdict(userId, p.getId(), Verdict.AC)) {
            userVerdict = Verdict.AC;
        }
        Set<TagDto> tags = p.getTags().stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .collect(Collectors.toSet());
        return new ProblemShortDto(p.getId(), p.getTitle(), p.getDifficulty(), tags, solved, userVerdict);
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
