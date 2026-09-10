package com.stackr.api.service;

import com.stackr.api.dto.request.SubmitRequest;
import com.stackr.api.dto.response.SubmissionDto;
import com.stackr.api.entity.Submission;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final SubmissionProcessor submissionProcessor;

    @Transactional
    public SubmissionDto submit(SubmitRequest req, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var problem = problemRepository.findById(req.problemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + req.problemId()));

        com.stackr.api.entity.Contest contest = null;
        if (req.contestId() != null) {
            contest = contestRepository.findById(req.contestId())
                    .orElseThrow(() -> new EntityNotFoundException("Contest not found: " + req.contestId()));
            if (contest.getStatus() == ContestStatus.FINISHED) {
                throw new BusinessException("Contest has already finished");
            }
        }

        var submission = Submission.builder()
                .user(user)
                .problem(problem)
                .contest(contest)
                .sourceCode(req.sourceCode())
                .verdict(Verdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        // Вызов через отдельный бин — @Async работает через Spring-прокси
        submissionProcessor.processAsync(
                submission.getId(),
                problem.getId(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb()
        );

        return toDto(submission, userId, false);
    }

    @Transactional(readOnly = true)
    public SubmissionDto getSubmission(Long id, Long currentUserId, boolean isOrganizer) {
        var s = submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + id));
        boolean canSeeCode = isOrganizer || s.getUser().getId().equals(currentUserId);
        return toDto(s, currentUserId, !canSeeCode);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getMySubmissions(Long userId, Long problemId, Long contestId, Pageable pageable) {
        return submissionRepository.findByUserIdWithFilters(userId, problemId, contestId, pageable)
                .map(s -> toDto(s, userId, false));
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getByProblem(Long problemId, Pageable pageable) {
        return submissionRepository.findByProblemId(problemId, pageable)
                .map(s -> toDto(s, null, false));
    }

    private SubmissionDto toDto(Submission s, Long viewerId, boolean hideCode) {
        return new SubmissionDto(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getUsername(),
                s.getProblem().getId(),
                s.getProblem().getTitle(),
                s.getVerdict(),
                s.getExecutionTimeMs(),
                s.getMemoryUsedMb(),
                s.getSubmittedAt(),
                hideCode ? null : s.getSourceCode()
        );
    }
}
