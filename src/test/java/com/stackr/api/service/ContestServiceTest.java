package com.stackr.api.service;

import com.stackr.api.dto.request.CreateContestRequest;
import com.stackr.api.dto.request.UpdateContestRequest;
import com.stackr.api.dto.response.ContestDetailDto;
import com.stackr.api.dto.response.LeaderboardEntryDto;
import com.stackr.api.entity.Contest;
import com.stackr.api.entity.ContestParticipation;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.ConflictException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContestService — unit tests")
class ContestServiceTest {

    @Mock ContestRepository contestRepository;
    @Mock ContestParticipationRepository participationRepository;
    @Mock UserRepository userRepository;
    @Mock ProblemRepository problemRepository;
    @Mock SubmissionRepository submissionRepository;
    @InjectMocks ContestService contestService;

    private User organizer;
    private User contestant;
    private Contest runningContest;
    private Contest finishedContest;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).username("org").role(Role.ORGANIZER).active(true).build();
        contestant = User.builder().id(2L).username("player").role(Role.CONTESTANT).active(true).build();

        runningContest = Contest.builder()
                .id(5L).title("Spring Contest")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(ContestStatus.RUNNING)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .build();
        runningContest.setCreatedAt(LocalDateTime.now());

        finishedContest = Contest.builder()
                .id(6L).title("Old Contest")
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .status(ContestStatus.FINISHED)
                .organizer(organizer)
                .problems(new ArrayList<>())
                .build();
        finishedContest.setCreatedAt(LocalDateTime.now());
    }

    // ── createContest ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createContest: успешное создание → ContestDetailDto")
    void createContest_success_returnsDto() {
        var req = new CreateContestRequest(
                "Spring Contest", "desc",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of(), true);
        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(contestRepository.save(any())).willReturn(runningContest);
        given(participationRepository.findLeaderboard(5L)).willReturn(List.of());

        ContestDetailDto dto = contestService.createContest(req, 1L);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.title()).isEqualTo("Spring Contest");
        then(contestRepository).should().save(any(Contest.class));
    }

    @Test
    @DisplayName("createContest: организатор не найден → EntityNotFoundException")
    void createContest_organizerNotFound_throws() {
        var req = new CreateContestRequest("T", "d",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), List.of(), true);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contestService.createContest(req, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── registerParticipant ───────────────────────────────────────────────────

    @Test
    @DisplayName("registerParticipant: успешная регистрация → участие сохранено")
    void registerParticipant_success_savesParticipation() {
        given(contestRepository.findById(5L)).willReturn(Optional.of(runningContest));
        given(participationRepository.existsByUserIdAndContestId(2L, 5L)).willReturn(false);
        given(userRepository.findById(2L)).willReturn(Optional.of(contestant));

        contestService.registerParticipant(5L, 2L);

        then(participationRepository).should().save(any(ContestParticipation.class));
    }

    @Test
    @DisplayName("registerParticipant: уже зарегистрирован → ConflictException (409)")
    void registerParticipant_alreadyRegistered_throwsConflict() {
        given(contestRepository.findById(5L)).willReturn(Optional.of(runningContest));
        given(participationRepository.existsByUserIdAndContestId(2L, 5L)).willReturn(true);

        assertThatThrownBy(() -> contestService.registerParticipant(5L, 2L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("registerParticipant: контест завершён → BusinessException (422)")
    void registerParticipant_contestFinished_throwsBusiness() {
        given(contestRepository.findById(6L)).willReturn(Optional.of(finishedContest));

        assertThatThrownBy(() -> contestService.registerParticipant(6L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finished");
    }

    @Test
    @DisplayName("registerParticipant: контест не найден → EntityNotFoundException")
    void registerParticipant_contestNotFound_throws() {
        given(contestRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contestService.registerParticipant(99L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getLeaderboard ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLeaderboard: возвращает отсортированный рейтинг с рангами")
    void getLeaderboard_returnsRankedEntries() {
        var cp1 = ContestParticipation.builder()
                .user(contestant).contest(runningContest)
                .score(200).penalty(5).submissionCount(3).build();
        cp1.setRegisteredAt(LocalDateTime.now());
        var cp2 = ContestParticipation.builder()
                .user(organizer).contest(runningContest)
                .score(100).penalty(10).submissionCount(1).build();
        cp2.setRegisteredAt(LocalDateTime.now());

        given(participationRepository.findLeaderboard(5L)).willReturn(List.of(cp1, cp2));

        List<LeaderboardEntryDto> board = contestService.getLeaderboard(5L);

        assertThat(board).hasSize(2);
        assertThat(board.get(0).rank()).isEqualTo(1);
        assertThat(board.get(0).score()).isEqualTo(200);
        assertThat(board.get(0).username()).isEqualTo("player");
        assertThat(board.get(1).rank()).isEqualTo(2);
        assertThat(board.get(1).score()).isEqualTo(100);
    }

    @Test
    @DisplayName("getLeaderboard: пустой контест → пустой список")
    void getLeaderboard_emptyContest_returnsEmptyList() {
        given(participationRepository.findLeaderboard(5L)).willReturn(List.of());

        assertThat(contestService.getLeaderboard(5L)).isEmpty();
    }

    // ── deleteContest ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteContest: не найден → EntityNotFoundException")
    void deleteContest_notFound_throws() {
        given(contestRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> contestService.deleteContest(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateContest — ownership check ───────────────────────────────────────

    @Test
    @DisplayName("updateContest: организатор обновляет свой контест → OK")
    void updateContest_organizerOwnsContest_succeeds() {
        var req = new UpdateContestRequest("New Title", null, null, null, null, null);
        given(contestRepository.findById(5L)).willReturn(Optional.of(runningContest));
        given(contestRepository.save(any())).willReturn(runningContest);
        given(participationRepository.existsByUserIdAndContestId(1L, 5L)).willReturn(false);

        ContestDetailDto dto = contestService.updateContest(5L, req, 1L);

        assertThat(dto.id()).isEqualTo(5L);
        then(contestRepository).should().save(any(Contest.class));
    }

    @Test
    @DisplayName("updateContest: организатор пытается обновить чужой контест → BusinessException")
    void updateContest_organizerNotOwner_throwsBusiness() {
        var req = new UpdateContestRequest("Hack", null, null, null, null, null);
        var otherUser = User.builder().id(3L).username("other").role(Role.CONTESTANT).active(true).build();
        given(contestRepository.findById(5L)).willReturn(Optional.of(runningContest));
        given(userRepository.findById(3L)).willReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> contestService.updateContest(5L, req, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("updateContest: ADMIN может обновить чужой контест")
    void updateContest_adminNotOwner_succeeds() {
        var admin = User.builder().id(99L).username("admin").role(Role.ADMIN).active(true).build();
        var req = new UpdateContestRequest("Admin Title", null, null, null, null, null);
        given(contestRepository.findById(5L)).willReturn(Optional.of(runningContest));
        given(userRepository.findById(99L)).willReturn(Optional.of(admin));
        given(contestRepository.save(any())).willReturn(runningContest);
        given(participationRepository.existsByUserIdAndContestId(99L, 5L)).willReturn(false);

        ContestDetailDto dto = contestService.updateContest(5L, req, 99L);

        assertThat(dto.id()).isEqualTo(5L);
        then(contestRepository).should().save(any(Contest.class));
    }

}