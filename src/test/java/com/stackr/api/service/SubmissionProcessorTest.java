package com.stackr.api.service;

import com.stackr.api.entity.Problem;
import com.stackr.api.entity.Submission;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionProcessor — unit tests")
class SubmissionProcessorTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock UserRepository userRepository;
    @Mock ContestParticipationRepository participationRepository;
    @Mock RunnerClient runnerClient;
    @Mock NotificationService notificationService;

    @InjectMocks SubmissionProcessor processor;

    private User user;
    private Problem problem;
    private Submission submission;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("coder").role(Role.CONTESTANT).active(true).rating(100).build();
        problem = Problem.builder().id(10L).title("Two Sum").difficulty(Difficulty.EASY)
                .timeLimitMs(2000).memoryLimitMb(256).build();
        submission = Submission.builder()
                .id(42L).user(user).problem(problem)
                .sourceCode("public class Main {}").verdict(Verdict.PENDING)
                .build();
    }

    private RunnerClient.RunnerResponse acResponse() {
        var r = new RunnerClient.RunnerResponse();
        r.setVerdict("AC");
        r.setExecutionTimeMs(150);
        r.setMemoryUsedMb(32);
        return r;
    }

    private RunnerClient.RunnerResponse waResponse() {
        var r = new RunnerClient.RunnerResponse();
        r.setVerdict("WA");
        r.setExecutionTimeMs(200);
        r.setMemoryUsedMb(32);
        return r;
    }

    // ── AC, первая решённая → рейтинг начисляется ─────────────────────────────

    @Test
    @DisplayName("processSubmission: AC, первый AC для юзера на задаче → рейтинг +10")
    void processSubmission_firstAc_updatesRating() {
        given(submissionRepository.findById(42L)).willReturn(Optional.of(submission));
        given(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).willReturn(List.of());
        given(runnerClient.run(eq(42L), any(), any(), anyInt(), anyInt())).willReturn(acResponse());
        given(submissionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(submissionRepository.countByUserIdAndProblemIdAndVerdict(1L, 10L, Verdict.AC)).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        processor.processSubmission(42L, 10L, 2000, 256);

        assertThat(submission.getVerdict()).isEqualTo(Verdict.AC);
        then(userRepository).should().save(argThat(u -> u.getRating() == 110));
    }

    // ── AC, уже решал раньше → двойного начисления нет ───────────────────────

    @Test
    @DisplayName("processSubmission: AC, но не первый AC (count=2) → рейтинг НЕ начисляется")
    void processSubmission_secondAc_doesNotUpdateRating() {
        given(submissionRepository.findById(42L)).willReturn(Optional.of(submission));
        given(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).willReturn(List.of());
        given(runnerClient.run(eq(42L), any(), any(), anyInt(), anyInt())).willReturn(acResponse());
        given(submissionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(submissionRepository.countByUserIdAndProblemIdAndVerdict(1L, 10L, Verdict.AC)).willReturn(2L);

        processor.processSubmission(42L, 10L, 2000, 256);

        assertThat(submission.getVerdict()).isEqualTo(Verdict.AC);
        // updateRating never called — userRepository.findById should not be touched
        then(userRepository).should(never()).findById(any());
        then(userRepository).should(never()).save(any());
    }

    // ── WA → рейтинг не трогается ─────────────────────────────────────────────

    @Test
    @DisplayName("processSubmission: WA вердикт → рейтинг и score не меняются")
    void processSubmission_wrongAnswer_noRatingChange() {
        given(submissionRepository.findById(42L)).willReturn(Optional.of(submission));
        given(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).willReturn(List.of());
        given(runnerClient.run(eq(42L), any(), any(), anyInt(), anyInt())).willReturn(waResponse());
        given(submissionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        processor.processSubmission(42L, 10L, 2000, 256);

        assertThat(submission.getVerdict()).isEqualTo(Verdict.WA);
        then(userRepository).should(never()).save(any());
        then(participationRepository).should(never()).save(any());
        then(submissionRepository).should(never()).countByUserIdAndProblemIdAndVerdict(any(), any(), any());
    }

    // ── Runner упал → RE вердикт ───────────────────────────────────────────────

    @Test
    @DisplayName("processSubmission: runner выбросил исключение → вердикт RE, сохраняется")
    void processSubmission_runnerThrows_setsInfrastructureError() {
        given(submissionRepository.findById(42L)).willReturn(Optional.of(submission));
        given(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).willReturn(List.of());
        given(runnerClient.run(eq(42L), any(), any(), anyInt(), anyInt()))
                .willThrow(new RuntimeException("Connection refused"));
        given(submissionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        processor.processSubmission(42L, 10L, 2000, 256);

        assertThat(submission.getVerdict()).isEqualTo(Verdict.ERROR);
        assertThat(submission.getErrorMessage()).contains("Runner unavailable");
        then(submissionRepository).should().save(submission);
    }

    // ── Submission не найдена → ранний выход ──────────────────────────────────

    @Test
    @DisplayName("processSubmission: submission не найдена → ничего не вызывается")
    void processSubmission_submissionNotFound_returnsEarly() {
        given(submissionRepository.findById(99L)).willReturn(Optional.empty());

        processor.processSubmission(99L, 10L, 2000, 256);

        then(runnerClient).should(never()).run(any(), any(), any(), anyInt(), anyInt());
        then(submissionRepository).should(never()).save(any());
    }
    @Test
    void repeatedAcceptedSolutionsDoNotInflateContestScore() {
        var contest = com.stackr.api.entity.Contest.builder().id(5L).build();
        submission.setContest(contest);
        var previousAc = com.stackr.api.entity.Submission.builder().user(user).problem(problem)
                .contest(contest).verdict(Verdict.AC).build();
        var wrong = com.stackr.api.entity.Submission.builder().user(user).problem(problem)
                .contest(contest).verdict(Verdict.WA).build();
        var participation = com.stackr.api.entity.ContestParticipation.builder().user(user).contest(contest).score(100).build();
        given(submissionRepository.findById(42L)).willReturn(Optional.of(submission));
        given(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).willReturn(List.of());
        given(runnerClient.run(eq(42L), any(), any(), anyInt(), anyInt())).willReturn(acResponse());
        given(submissionRepository.countByUserIdAndProblemIdAndVerdict(1L, 10L, Verdict.AC)).willReturn(2L);
        given(participationRepository.findByUserIdAndContestId(1L, 5L)).willReturn(Optional.of(participation));
        given(submissionRepository.findAllByContestId(5L)).willReturn(List.of(previousAc, wrong, submission));
        processor.processSubmission(42L, 10L, 2000, 256);
        assertThat(participation.getScore()).isEqualTo(100);
        assertThat(participation.getSubmissionCount()).isEqualTo(3);
    }

}
