package com.stackr.api.service;

import com.stackr.api.entity.Submission;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed queue processor. One scheduled worker processes committed PENDING rows.
 * A row lock prevents the same submission from being processed twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionProcessor {

    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;
    private final ContestParticipationRepository participationRepository;
    private final RunnerClient runnerClient;
    private final NotificationService notificationService;

    public RunnerClient getRunnerClient() { return runnerClient; }

    @Transactional
    public void processNext() {
        var pending = submissionRepository.findPendingForProcessing(Verdict.PENDING, PageRequest.of(0, 1));
        if (pending.isEmpty()) return;
        var s = pending.get(0);
        processSubmission(s.getId(), s.getProblem().getId(), s.getProblem().getTimeLimitMs(), s.getProblem().getMemoryLimitMb());
    }

    @Transactional
    public void processSubmission(Long submissionId, Long problemId, int timeLimitMs, int memoryLimitMb) {
        var submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("Submission {} not found for async processing", submissionId);
            return;
        }

        if (submission.getVerdict() != Verdict.PENDING) return;

        var testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);

        try {
            var result = runnerClient.run(submissionId, submission.getSourceCode(),
                    testCases, timeLimitMs, memoryLimitMb);

            submission.setVerdict(Verdict.valueOf(result.getVerdict()));
            submission.setExecutionTimeMs(result.getExecutionTimeMs());
            submission.setMemoryUsedMb(result.getMemoryUsedMb());
            submission.setErrorMessage(result.getErrorMessage());
        } catch (Exception e) {
            log.error("Runner error for submission {}: {}", submissionId, e.getMessage());
            submission.setVerdict(Verdict.ERROR);
            submission.setErrorMessage("Runner unavailable");
        }

        submissionRepository.save(submission);

        // Send notification about verdict
        sendVerdictNotification(submission, problemId);

        // isFirstAc check AFTER saving the verdict, not before:
        // count how many AC submissions now exist for this user+problem.
        // If exactly 1 (this one), it's the first AC — prevents double-credit race condition.
        if (submission.getVerdict() == Verdict.AC) {
            long acCount = submissionRepository.countByUserIdAndProblemIdAndVerdict(
                    submission.getUser().getId(), problemId, Verdict.AC);
            if (acCount == 1L) {
                updateRating(submission.getUser().getId());
            }
        }
        if (submission.getContest() != null) updateContestScore(submission);
    }

    private void sendVerdictNotification(Submission submission, Long problemId) {
        Long userId = submission.getUser().getId();
        String problemTitle = submission.getProblem() != null
                ? submission.getProblem().getTitle()
                : "задача #" + problemId;
        String link = "/problems/" + problemId;

        String message = switch (submission.getVerdict()) {
            case AC  -> "✓ Принято: «" + problemTitle + "»";
            case WA  -> "✗ Неверный ответ: «" + problemTitle + "»";
            case TLE -> "⏱ Превышен лимит времени: «" + problemTitle + "»";
            case CE  -> "⚙ Ошибка компиляции: «" + problemTitle + "»";
            case ERROR -> "Проверка временно недоступна: «" + problemTitle + "»";
            case RE  -> "💥 Ошибка выполнения: «" + problemTitle + "»";
            default  -> "Посылка проверена: «" + problemTitle + "»";
        };

        notificationService.create(userId, message, link);
    }

    private void updateRating(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setRating(u.getRating() + 10);
            userRepository.save(u);
            log.debug("Rating updated for user {}: +10 (total: {})", userId, u.getRating());
        });
    }

    private void updateContestScore(Submission submission) {
        participationRepository
                .findByUserIdAndContestId(submission.getUser().getId(), submission.getContest().getId())
                .ifPresent(cp -> {
                    var submissions = submissionRepository.findAllByContestId(submission.getContest().getId()).stream()
                            .filter(s -> s.getUser().getId().equals(submission.getUser().getId()))
                            .filter(s -> s.getVerdict() != Verdict.PENDING && s.getVerdict() != Verdict.ERROR).toList();
                    long solved = submissions.stream().filter(s -> s.getVerdict() == Verdict.AC)
                            .map(s -> s.getProblem().getId()).distinct().count();
                    cp.setScore(Math.toIntExact(solved * 100));
                    cp.setSubmissionCount(submissions.size());
                    participationRepository.save(cp);
                });
    }
}
