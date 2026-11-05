package com.stackr.api.service;

import com.stackr.api.dto.request.RunCodeRequest;
import java.time.LocalDateTime;
import com.stackr.api.dto.request.SubmitRequest;
import com.stackr.api.dto.response.RunCodeResult;
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
    private final ContestParticipationRepository participationRepository;

    @Transactional
    public SubmissionDto submit(SubmitRequest req, Long userId) {
        var user = userRepository.findForSubmission(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var problem = problemRepository.findById(req.problemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + req.problemId()));

        com.stackr.api.entity.Contest contest = null;
        if (req.contestId() != null) {
            contest = contestRepository.findById(req.contestId())
                    .orElseThrow(() -> new EntityNotFoundException("Contest not found: " + req.contestId()));
            if (contest.getStatus() == ContestStatus.FINISHED || !LocalDateTime.now().isBefore(contest.getEndTime())) {
                throw new BusinessException("Contest has already finished");
            }
            if (LocalDateTime.now().isBefore(contest.getStartTime())) {
                throw new BusinessException("Contest has not started");
            }
            if (!participationRepository.existsByUserIdAndContestId(userId, contest.getId())) {
                throw new BusinessException("Register for the contest before submitting");
            }
            if (contest.getProblems().stream().noneMatch(p -> p.getId().equals(problem.getId()))) {
                throw new BusinessException("Problem does not belong to this contest");
            }
        }

        if (submissionRepository.countByUserIdAndVerdict(userId, Verdict.PENDING) >= 5) {
            throw new BusinessException("У вас уже пять решений в очереди. Дождитесь результатов проверки.");
        }
        var submission = Submission.builder()
                .user(user)
                .problem(problem)
                .contest(contest)
                .sourceCode(req.sourceCode())
                .verdict(Verdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        // The database queue worker sees this row only after commit.
        return toDto(submission, userId, false);
    }

    @Transactional(readOnly = true)
    public SubmissionDto getSubmission(Long id, Long currentUserId, boolean isAdmin) {
        var s = submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + id));
        boolean canSeeCode = isAdmin || s.getUser().getId().equals(currentUserId)
                || s.getProblem().getAuthor().getId().equals(currentUserId);
        return toDto(s, currentUserId, !canSeeCode);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getMySubmissions(Long userId, Long problemId, Long contestId, Pageable pageable) {
        return submissionRepository.findByUserIdWithFilters(userId, problemId, contestId, pageable)
                .map(s -> toDto(s, userId, false));
    }

    public RunCodeResult runCode(RunCodeRequest req) {
        var problem = problemRepository.findById(req.problemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + req.problemId()));
        var result = submissionProcessor.getRunnerClient().runCustom(
                req.sourceCode(),
                req.customInput() != null ? req.customInput() : "",
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb()
        );
        return new RunCodeResult(
                result.getStdout(),
                result.getStderr(),
                result.getElapsedMs(),
                result.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getByProblem(Long problemId, Pageable pageable) {
        return submissionRepository.findByProblemId(problemId, pageable)
                .map(s -> toDto(s, null, false));
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getByContest(Long contestId, Pageable pageable) {
        return submissionRepository.findByContestId(contestId, pageable)
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
                hideCode ? null : s.getSourceCode(),
                !hideCode && (s.getVerdict() == Verdict.CE || s.getVerdict() == Verdict.ERROR) ? s.getErrorMessage() : null
        );
    }
}
