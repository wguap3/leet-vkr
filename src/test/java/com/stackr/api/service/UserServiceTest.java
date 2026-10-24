package com.stackr.api.service;

import com.stackr.api.dto.response.TagProgressDto;
import com.stackr.api.dto.response.UserProfileDto;
import com.stackr.api.entity.User;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.SubmissionRepository;
import com.stackr.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import com.stackr.api.dto.request.ChangePasswordRequest;
import com.stackr.api.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — unit tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice")
                .email("alice@test.com").role(Role.CONTESTANT)
                .rating(750).active(true).build();
        alice.setRegisteredAt(LocalDateTime.now().minusDays(30));

        bob = User.builder().id(2L).username("bob")
                .email("bob@test.com").role(Role.ORGANIZER)
                .rating(1200).active(true).build();
        bob.setRegisteredAt(LocalDateTime.now().minusDays(10));
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfile: пользователь найден → UserProfileDto с рейтингом")
    void getProfile_found_returnsDto() {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(submissionRepository.countDistinctSolvedByUserId(1L, Verdict.AC)).willReturn(12L);
        given(submissionRepository.countByUserId(1L)).willReturn(30L);

        UserProfileDto dto = userService.getProfile(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.rating()).isEqualTo(750);
        assertThat(dto.totalAC()).isEqualTo(12L);
        assertThat(dto.totalSubmissions()).isEqualTo(30L);
        assertThat(dto.ratingTier()).isEqualTo("Участник");
        assertThat(dto.ratingTierIcon()).isEqualTo("⚡");
    }

    @Test
    @DisplayName("getProfile: пользователь не найден → EntityNotFoundException")
    void getProfile_notFound_throws() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getLeaderboard ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLeaderboard: две страницы пользователей → batch-запросы, правильный порядок")
    void getLeaderboard_usesBatchQueries() {
        var page = new PageImpl<>(List.of(alice, bob));
        given(userRepository.findAllByOrderByRatingDesc(any())).willReturn(page);

        // Batch mock: AC counts — [userId, acCount]
        given(submissionRepository.countDistinctSolvedByUserIdsAndVerdict(anyList(), eq(Verdict.AC)))
                .willReturn(List.of(new Object[]{1L, 12L}, new Object[]{2L, 25L}));

        // Batch mock: total submission counts — [userId, total]
        given(submissionRepository.countSubmissionsByUserIds(anyList()))
                .willReturn(List.of(new Object[]{1L, 30L}, new Object[]{2L, 60L}));

        var result = userService.getLeaderboard(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        UserProfileDto a = result.getContent().get(0);
        UserProfileDto b = result.getContent().get(1);

        assertThat(a.username()).isEqualTo("alice");
        assertThat(a.totalAC()).isEqualTo(12L);
        assertThat(a.totalSubmissions()).isEqualTo(30L);
        assertThat(b.username()).isEqualTo("bob");
        assertThat(b.totalAC()).isEqualTo(25L);
        assertThat(b.totalSubmissions()).isEqualTo(60L);

        // Убеждаемся, что вызвано ровно по 1 batch-запросу, а не по 2 (N+1 устранён)
        then(submissionRepository).should(times(1))
                .countDistinctSolvedByUserIdsAndVerdict(anyList(), eq(Verdict.AC));
        then(submissionRepository).should(times(1))
                .countSubmissionsByUserIds(anyList());
        then(submissionRepository).should(never())
                .countDistinctSolvedByUserId(anyLong(), any());
        then(submissionRepository).should(never())
                .countByUserId(anyLong());
    }

    @Test
    @DisplayName("getLeaderboard: пустой список → немедленно возвращает пустую страницу")
    void getLeaderboard_empty_returnsEmpty() {
        given(userRepository.findAllByOrderByRatingDesc(any()))
                .willReturn(new PageImpl<>(List.of()));

        var result = userService.getLeaderboard(PageRequest.of(0, 20));

        assertThat(result).isEmpty();
        then(submissionRepository).should(never()).countDistinctSolvedByUserIdsAndVerdict(any(), any());
    }

    // ── getTopicProgress ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopicProgress: объединяет solved и total по тегу")
    void getTopicProgress_mergesSolvedAndTotal() {
        // [tagId, tagName, count]
        given(submissionRepository.countSolvedPerTagByUserId(1L))
                .willReturn(List.of(new Object[]{10L, "Arrays", 5L}));
        given(submissionRepository.countProblemsPerTag())
                .willReturn(List.of(
                        new Object[]{10L, "Arrays", 8L},
                        new Object[]{20L, "Graphs", 6L}
                ));

        List<TagProgressDto> progress = userService.getTopicProgress(1L);

        // Sorted by tagName → Arrays first, Graphs second
        assertThat(progress).hasSize(2);
        TagProgressDto arrays = progress.stream().filter(p -> p.tagName().equals("Arrays")).findFirst().orElseThrow();
        TagProgressDto graphs = progress.stream().filter(p -> p.tagName().equals("Graphs")).findFirst().orElseThrow();

        assertThat(arrays.solved()).isEqualTo(5L);
        assertThat(arrays.total()).isEqualTo(8L);
        assertThat(graphs.solved()).isEqualTo(0L); // not in solvedRows
        assertThat(graphs.total()).isEqualTo(6L);
    }
    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: верный старый пароль → пароль обновляется")
    void changePassword_correctOldPassword_updatesHash() {
        user.setPasswordHash("$2a$10$hashed");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPass", "$2a$10$hashed")).willReturn(true);
        given(passwordEncoder.encode("newPass123")).willReturn("$2a$10$newHash");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        userService.changePassword(1L, new ChangePasswordRequest("oldPass", "newPass123"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newHash");
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("changePassword: неверный старый пароль → BusinessException")
    void changePassword_wrongOldPassword_throwsBusiness() {
        user.setPasswordHash("$2a$10$hashed");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPass", "$2a$10$hashed")).willReturn(false);

        assertThatThrownBy(() ->
                userService.changePassword(1L, new ChangePasswordRequest("wrongPass", "newPass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Неверный текущий пароль");

        then(userRepository).should(never()).save(any());
    }
}