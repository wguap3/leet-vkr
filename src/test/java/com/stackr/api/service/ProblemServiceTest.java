package com.stackr.api.service;

import com.stackr.api.dto.request.CreateProblemRequest;
import com.stackr.api.dto.request.UpdateProblemRequest;
import com.stackr.api.dto.response.ProblemDetailDto;
import com.stackr.api.dto.response.ProblemShortDto;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemService — unit tests")
class ProblemServiceTest {

    @Mock ProblemRepository problemRepository;
    @Mock UserRepository userRepository;
    @Mock TagRepository tagRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock SubmissionRepository submissionRepository;
    @InjectMocks ProblemService problemService;

    private User author;
    private Problem problem;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("organizer").role(Role.ORGANIZER).active(true).build();
        problem = Problem.builder()
                .id(10L).title("Two Sum").description("Given an array...")
                .difficulty(Difficulty.EASY).timeLimitMs(2000).memoryLimitMb(256)
                .tags(new HashSet<>()).author(author).visible(true)
                .build();
    }

    // ── getProblems ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProblems: возвращает страницу ProblemShortDto")
    void getProblems_returnsPage() {
        var page = new PageImpl<>(List.of(problem));
        given(problemRepository.findByFilters(null, null, PageRequest.of(0, 10))).willReturn(page);
        given(submissionRepository.countDistinctUsersByProblemIdAndVerdict(10L, Verdict.AC)).willReturn(5L);
        given(submissionRepository.existsByUserIdAndProblemIdAndVerdict(1L, 10L, Verdict.AC)).willReturn(true);

        var result = problemService.getProblems(null, null, 1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        ProblemShortDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.title()).isEqualTo("Two Sum");
        assertThat(dto.solvedCount()).isEqualTo(5L);
        assertThat(dto.userVerdict()).isEqualTo(Verdict.AC);
    }

    // ── getProblemById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProblemById: задача найдена → возвращает ProblemDetailDto")
    void getProblemById_found_returnsDetail() {
        given(problemRepository.findById(10L)).willReturn(Optional.of(problem));
        given(testCaseRepository.findByProblemIdAndIsPublicTrueOrderByOrderIndexAsc(10L))
                .willReturn(List.of());

        ProblemDetailDto dto = problemService.getProblemById(10L);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.title()).isEqualTo("Two Sum");
        assertThat(dto.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(dto.publicTestCases()).isEmpty();
    }

    @Test
    @DisplayName("getProblemById: задача не найдена → EntityNotFoundException")
    void getProblemById_notFound_throws() {
        given(problemRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblemById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createProblem ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProblem: успешное создание → ProblemDetailDto")
    void createProblem_success_returnsProblemDetailDto() {
        var req = new CreateProblemRequest(
                "Two Sum", "Given an array...", Difficulty.EASY, 2000, 256, Set.of(), null);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(problemRepository.save(any())).willReturn(problem);

        ProblemDetailDto dto = problemService.createProblem(req, 1L);

        assertThat(dto.title()).isEqualTo("Two Sum");
        assertThat(dto.difficulty()).isEqualTo(Difficulty.EASY);
        then(problemRepository).should().save(any(Problem.class));
    }

    @Test
    @DisplayName("createProblem: автор не найден → EntityNotFoundException")
    void createProblem_authorNotFound_throws() {
        var req = new CreateProblemRequest("T", "d", Difficulty.HARD, 3000, 512, Set.of(), null);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.createProblem(req, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateProblem ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProblem: обновляет поля и возвращает ProblemDetailDto")
    void updateProblem_updatesFields() {
        var req = new UpdateProblemRequest("New Title", null, Difficulty.HARD, null, null, null, null);
        given(problemRepository.findById(10L)).willReturn(Optional.of(problem));
        given(problemRepository.save(any())).willReturn(problem);
        given(testCaseRepository.findByProblemIdAndIsPublicTrueOrderByOrderIndexAsc(10L))
                .willReturn(List.of());

        ProblemDetailDto dto = problemService.updateProblem(10L, req);

        assertThat(problem.getTitle()).isEqualTo("New Title");
        assertThat(problem.getDifficulty()).isEqualTo(Difficulty.HARD);
    }

    // ── deleteProblem ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProblem: задача существует → удаляется")
    void deleteProblem_exists_deletesSuccessfully() {
        given(problemRepository.existsById(10L)).willReturn(true);

        problemService.deleteProblem(10L);

        then(problemRepository).should().deleteById(10L);
    }

    @Test
    @DisplayName("deleteProblem: задача не найдена → EntityNotFoundException")
    void deleteProblem_notFound_throws() {
        given(problemRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> problemService.deleteProblem(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── setVisibility ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("setVisibility: устанавливает флаг visible и сохраняет")
    void setVisibility_savesCorrectFlag() {
        problem.setVisible(false);
        given(problemRepository.findById(10L)).willReturn(Optional.of(problem));
        given(problemRepository.save(any())).willReturn(problem);

        problemService.setVisibility(10L, true);

        assertThat(problem.isVisible()).isTrue();
        then(problemRepository).should().save(problem);
    }
}
