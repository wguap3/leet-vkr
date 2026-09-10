package com.stackr.api.service;

import com.stackr.api.dto.request.SubmitRequest;
import com.stackr.api.dto.response.SubmissionDto;
import com.stackr.api.entity.*;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService — unit tests")
class SubmissionServiceTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock ProblemRepository problemRepository;
    @Mock ContestRepository contestRepository;
    @Mock UserRepository userRepository;
    @Mock SubmissionProcessor submissionProcessor;
    @InjectMocks SubmissionService submissionService;

    private User user;
    private Problem problem;
    private Submission pendingSubmission;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").role(Role.CONTESTANT).active(true).build();
        problem = Problem.builder()
                .id(10L).title("Two Sum").description("desc")
                .difficulty(com.stackr.api.entity.enums.Difficulty.EASY)
                .timeLimitMs(2000).memoryLimitMb(256)
                .tags(new HashSet<>()).author(user).visible(true)
                .build();
        pendingSubmission = Submission.builder()
                .id(100L).user(user).problem(problem)
                .sourceCode("class Solution {}").verdict(Verdict.PENDING)
                .build();
        // simulate @PrePersist
        pendingSubmission.setSubmittedAt(LocalDateTime.now());
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit: без контеста → возвращает SubmissionDto с PENDING")
    void submit_noContest_returnsPendingSubmission() {
        var req = new SubmitRequest(10L, "class Solution {}", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(problemRepository.findById(10L)).willReturn(Optional.of(problem));
        given(submissionRepository.save(any())).willReturn(pendingSubmission);

        SubmissionDto dto = submissionService.submit(req, 1L);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.verdict()).isEqualTo(Verdict.PENDING);
        assertThat(dto.problemId()).isEqualTo(10L);
        // убеждаемся что async-процессор был вызван
        then(submissionProcessor).should().processAsync(100L, 10L, 2000, 256);
    }

    @Test
    @DisplayName("submit: контест FINISHED → BusinessException (422)")
    void submit_contestFinished_throwsBusinessException() {
        var contest = Contest.builder().id(5L).status(ContestStatus.FINISHED)
                .organizer(user).problems(new java.util.ArrayList<>()).build();
        contest.setCreatedAt(LocalDateTime.now());
        var req = new SubmitRequest(10L, "code", 5L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(problemRepository.findById(10L)).willReturn(Optional.of(problem));
        given(contestRepository.findById(5L)).willReturn(Optional.of(contest));

        assertThatThrownBy(() -> submissionService.submit(req, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finished");
    }

    @Test
    @DisplayName("submit: пользователь не найден → EntityNotFoundException")
    void submit_userNotFound_throws() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.submit(new SubmitRequest(10L, "code", null), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("submit: задача не найдена → EntityNotFoundException")
    void submit_problemNotFound_throws() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(problemRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.submit(new SubmitRequest(99L, "code", null), 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getSubmission ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSubmission: владелец видит исходный код")
    void getSubmission_owner_canSeeCode() {
        var sub = Submission.builder().id(1L).user(user).problem(problem)
                .verdict(Verdict.AC).sourceCode("secret code").build();
        sub.setSubmittedAt(LocalDateTime.now());
        given(submissionRepository.findById(1L)).willReturn(Optional.of(sub));

        SubmissionDto dto = submissionService.getSubmission(1L, 1L, false);

        assertThat(dto.sourceCode()).isEqualTo("secret code");
    }

    @Test
    @DisplayName("getSubmission: чужой пользователь → код скрыт (null)")
    void getSubmission_stranger_codeHidden() {
        var sub = Submission.builder().id(1L).user(user).problem(problem)
                .verdict(Verdict.AC).sourceCode("secret code").build();
        sub.setSubmittedAt(LocalDateTime.now());
        given(submissionRepository.findById(1L)).willReturn(Optional.of(sub));

        SubmissionDto dto = submissionService.getSubmission(1L, 999L, false);

        assertThat(dto.sourceCode()).isNull();
    }

    @Test
    @DisplayName("getSubmission: ORGANIZER видит код независимо от авторства")
    void getSubmission_organizer_canSeeCode() {
        var sub = Submission.builder().id(1L).user(user).problem(problem)
                .verdict(Verdict.AC).sourceCode("secret code").build();
        sub.setSubmittedAt(LocalDateTime.now());
        given(submissionRepository.findById(1L)).willReturn(Optional.of(sub));

        SubmissionDto dto = submissionService.getSubmission(1L, 999L, true);

        assertThat(dto.sourceCode()).isEqualTo("secret code");
    }

    @Test
    @DisplayName("getSubmission: посылка не найдена → EntityNotFoundException")
    void getSubmission_notFound_throws() {
        given(submissionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getSubmission(99L, 1L, false))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
