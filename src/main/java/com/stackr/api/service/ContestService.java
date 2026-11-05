package com.stackr.api.service;

import com.stackr.api.dto.request.CreateContestRequest;
import com.stackr.api.dto.request.UpdateContestRequest;
import com.stackr.api.dto.response.*;
import com.stackr.api.entity.Contest;
import com.stackr.api.entity.ContestParticipation;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.ConflictException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<ContestShortDto> getContests(ContestStatus status, Long userId, boolean isOrganizerOrAdmin) {
        return getContests(status, userId, isOrganizerOrAdmin, false);
    }

    @Transactional(readOnly = true)
    public List<ContestShortDto> getContests(ContestStatus status, Long userId, boolean isOrganizerOrAdmin, boolean mine) {
        boolean admin = userId != null && userRepository.findById(userId).map(u -> u.getRole() == Role.ADMIN).orElse(false);
        Set<Long> joined = mine && userId != null
                ? new HashSet<>(participationRepository.findContestIdsByUserId(userId)) : Set.of();
        var contests = contestRepository.findAccessible(null, userId, admin).stream()
                .filter(c -> effectiveStatus(c) != ContestStatus.FINISHED)
                .filter(c -> mine ? (c.getOrganizer().getId().equals(userId) || joined.contains(c.getId())) : c.isPublic())
                .filter(c -> status == null || effectiveStatus(c) == status).toList();
        if (contests.isEmpty()) return List.of();

        List<Long> ids = contests.stream().map(Contest::getId).toList();
        Map<Long, Long> participantMap = new HashMap<>();
        contestRepository.countParticipantsInContests(ids)
                .forEach(row -> participantMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));

        return contests.stream()
                .map(c -> toShortDto(c, participantMap.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContestDetailDto getContestById(Long id, Long userId) {
        var contest = findOrThrow(id);
        boolean registered = userId != null && participationRepository.existsByUserIdAndContestId(userId, id);
        return toDetailDto(contest, registered, userId);
    }

    @Transactional(readOnly = true)
    public ContestDetailDto getContestByInviteToken(String token, Long userId) {
        var contest = contestRepository.findByInviteToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite link"));
        boolean registered = userId != null && participationRepository.existsByUserIdAndContestId(userId, contest.getId());
        return toDetailDto(contest, registered, userId);
    }

    @Transactional
    public ContestDetailDto rotateInviteToken(Long id, Long userId) {
        var contest = findOrThrow(id);
        if (contest.isPublic()) throw new BusinessException("У публичного контеста нет приватной ссылки");
        contest.setInviteToken(UUID.randomUUID().toString());
        return toDetailDto(contest, false, userId);
    }

    @Transactional
    public ContestDetailDto registerByInviteToken(String token, Long userId) {
        var contest = contestRepository.findByInviteToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite link"));
        if (!java.time.LocalDateTime.now().isBefore(contest.getEndTime())) {
            throw new BusinessException("Contest is already finished");
        }
        if (!participationRepository.existsByUserIdAndContestId(userId, contest.getId())) {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            var participation = ContestParticipation.builder()
                    .user(user)
                    .contest(contest)
                    .build();
            participationRepository.save(participation);
        }
        return toDetailDto(contest, true, userId);
    }

    @Transactional
    public ContestDetailDto createContest(CreateContestRequest req, Long organizerId) {
        validateInterval(req.startTime(), req.endTime());
        var organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Problem> problems = resolveProblems(req.problemIds(), organizerId);
        boolean isPublic = req.isPublic() != null ? req.isPublic() : true;
        var contest = Contest.builder()
                .title(req.title())
                .description(req.description())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .problems(problems)
                .organizer(organizer)
                .isPublic(isPublic)
                .inviteToken(isPublic ? null : UUID.randomUUID().toString())
                .build();
        contest = contestRepository.save(contest);
        return toDetailDto(contest, false, organizerId);
    }

    @Transactional
    public ContestDetailDto updateContest(Long id, UpdateContestRequest req, Long userId) {
        var contest = findOrThrow(id);

        if (!contest.getOrganizer().getId().equals(userId)) {
            var caller = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            if (caller.getRole() != Role.ADMIN) {
                throw new BusinessException("You are not authorized to update this contest");
            }
        }

        validateInterval(req.startTime() != null ? req.startTime() : contest.getStartTime(),
                req.endTime() != null ? req.endTime() : contest.getEndTime());
        boolean hasHistory = submissionRepository.existsByContestId(id);
        if (hasHistory && ((req.startTime() != null && !req.startTime().equals(contest.getStartTime()))
                || (req.endTime() != null && !req.endTime().equals(contest.getEndTime())))) {
            throw new ConflictException("Нельзя менять время контеста после получения посылок");
        }
        if (req.title() != null) {
            if (req.title().isBlank()) throw new BusinessException("Укажите название контеста");
            contest.setTitle(req.title());
        }
        if (req.description() != null) contest.setDescription(req.description());
        if (req.startTime() != null) contest.setStartTime(req.startTime());
        if (req.endTime() != null) contest.setEndTime(req.endTime());
        if (req.problemIds() != null) {
            var current = contest.getProblems().stream().map(Problem::getId).collect(Collectors.toSet());
            var next = new HashSet<>(req.problemIds());
            if (!current.equals(next)) {
                if (hasHistory) throw new ConflictException("Нельзя менять набор задач после получения посылок");
                contest.setProblems(resolveProblems(req.problemIds(), userId));
            }
        }
        if (req.isPublic() != null) {
            boolean wasPublic = contest.isPublic();
            boolean nowPublic = req.isPublic();
            contest.setPublic(nowPublic);
            // Generate invite token if switching to private
            if (!nowPublic && (wasPublic || contest.getInviteToken() == null)) {
                contest.setInviteToken(UUID.randomUUID().toString());
            } else if (nowPublic) {
                contest.setInviteToken(null);
            }
        }
        contest = contestRepository.save(contest);
        boolean registered = participationRepository.existsByUserIdAndContestId(userId, id);
        return toDetailDto(contest, registered, userId);
    }

    @Transactional
    public void deleteContest(Long id, Long requesterId, boolean isAdmin) {
        var contest = findOrThrow(id);
        if (!isAdmin && !contest.getOrganizer().getId().equals(requesterId)) {
            throw new BusinessException("You are not authorized to delete this contest");
        }
        if (submissionRepository.existsByContestId(id)) throw new ConflictException("У контеста есть посылки. Историю результатов необходимо сохранить.");
        participationRepository.deleteByContestId(id);
        contestRepository.deleteById(id);
    }

    @Transactional
    public void registerParticipant(Long contestId, Long userId) {
        var contest = findOrThrow(contestId);
        if (!java.time.LocalDateTime.now().isBefore(contest.getEndTime())) {
            throw new BusinessException("Contest is already finished");
        }
        if (participationRepository.existsByUserIdAndContestId(userId, contestId)) {
            throw new ConflictException("Already registered in this contest");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var participation = ContestParticipation.builder()
                .user(user)
                .contest(contest)
                .build();
        participationRepository.save(participation);
    }

    @Transactional
    public void removeParticipant(Long contestId, Long userId) {
        findOrThrow(contestId);
        var participation = participationRepository.findByUserIdAndContestId(userId, contestId)
                .orElseThrow(() -> new EntityNotFoundException("Участник не найден"));
        if (submissionRepository.existsByUserIdAndContestId(userId, contestId)) {
            throw new ConflictException("У участника уже есть посылки. Нельзя удалить его результаты.");
        }
        participationRepository.delete(participation);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(Long contestId) {
        var standings = getStandings(contestId);
        var attempts = submissionRepository.findAllByContestId(contestId).stream()
                .filter(s -> s.getVerdict() != Verdict.PENDING && s.getVerdict() != Verdict.ERROR)
                .collect(Collectors.groupingBy(s -> s.getUser().getId(), Collectors.counting()));
        return standings.rows().stream().map(r -> new LeaderboardEntryDto(r.rank(), r.userId(), r.username(),
                r.solved() * 100, r.penalty(), attempts.getOrDefault(r.userId(), 0L).intValue())).toList();
    }

    @Transactional(readOnly = true)
    public ContestAnalyticsDto getAnalytics(Long contestId) {
        var standings = getStandings(contestId);
        var submissions = submissionRepository.findAllByContestId(contestId).stream()
                .filter(s -> s.getVerdict() != Verdict.PENDING && s.getVerdict() != Verdict.ERROR).toList();
        var headers = standings.problems().stream().map(p -> new ContestAnalyticsDto.ProblemHeader(p.id(), p.title())).toList();
        var rows = standings.rows().stream().map(row -> {
            Map<Long, ContestAnalyticsDto.ProblemResult> results = new HashMap<>();
            for (var problem : standings.problems()) {
                int attempts = (int) submissions.stream().filter(s -> s.getUser().getId().equals(row.userId())
                        && s.getProblem().getId().equals(problem.id())).count();
                var cell = row.cells().get(problem.id());
                results.put(problem.id(), new ContestAnalyticsDto.ProblemResult(attempts, cell != null && cell.solved(), cell == null ? null : cell.solvedAt()));
            }
            return new ContestAnalyticsDto.ParticipantRow(row.userId(), row.username(), results);
        }).toList();
        return new ContestAnalyticsDto(contestId, standings.contestTitle(), headers, rows);
    }

    @Transactional(readOnly = true)
    public ContestStandingsDto getStandings(Long contestId) {
        var contest = findOrThrow(contestId);
        var submissions = submissionRepository.findAllByContestId(contestId);

        List<com.stackr.api.entity.Problem> problems = contest.getProblems();
        List<ContestStandingsDto.ProblemHeader> headers = new ArrayList<>();
        for (int i = 0; i < problems.size(); i++) {
            var p = problems.get(i);
            headers.add(new ContestStandingsDto.ProblemHeader(p.getId(), p.getTitle(),
                    String.valueOf((char) ('A' + i))));
        }
        Set<Long> problemIds = problems.stream()
                .map(com.stackr.api.entity.Problem::getId).collect(Collectors.toSet());

        Set<Long> userIds = new LinkedHashSet<>();
        Map<Long, String> usernames = new HashMap<>();

        participationRepository.findLeaderboard(contestId).forEach(cp -> {
            userIds.add(cp.getUser().getId());
            usernames.put(cp.getUser().getId(), cp.getUser().getUsername());
        });
        submissions.forEach(s -> {
            userIds.add(s.getUser().getId());
            usernames.put(s.getUser().getId(), s.getUser().getUsername());
        });

        Map<Long, Map<Long, List<com.stackr.api.entity.Submission>>> grouped = new HashMap<>();
        for (var s : submissions) {
            Long uid = s.getUser().getId();
            Long pid = s.getProblem().getId();
            if (!problemIds.contains(pid)) continue;
            grouped.computeIfAbsent(uid, k -> new HashMap<>())
                   .computeIfAbsent(pid, k -> new ArrayList<>())
                   .add(s);
        }

        List<ContestStandingsDto.StandingsRow> rows = new ArrayList<>();
        for (Long uid : userIds) {
            Map<Long, ContestStandingsDto.ProblemCell> cells = new HashMap<>();
            int totalSolved = 0;
            int totalPenalty = 0;

            for (var p : problems) {
                Long pid = p.getId();
                var subs = grouped.getOrDefault(uid, Map.of()).getOrDefault(pid, List.of());

                int wrongBefore = 0;
                boolean solved = false;
                Integer solvedAt = null;

                for (var s : subs) {
                    if (s.getVerdict() == com.stackr.api.entity.enums.Verdict.AC) {
                        solved = true;
                        if (contest.getStartTime() != null) {
                            solvedAt = (int) java.time.Duration.between(
                                    contest.getStartTime(), s.getSubmittedAt()).toMinutes();
                        }
                        break;
                    } else if (s.getVerdict() != Verdict.PENDING && s.getVerdict() != Verdict.ERROR) {
                        wrongBefore++;
                    }
                }

                if (solved) {
                    totalSolved++;
                    int pen = (solvedAt != null ? solvedAt : 0) + 20 * wrongBefore;
                    totalPenalty += pen;
                }
                if (!subs.isEmpty()) {
                    cells.put(pid, new ContestStandingsDto.ProblemCell(solved, wrongBefore, solvedAt));
                }
            }

            rows.add(new ContestStandingsDto.StandingsRow(
                    0, uid, usernames.get(uid), totalSolved, totalPenalty, cells));
        }

        rows.sort(Comparator.<ContestStandingsDto.StandingsRow>comparingInt(r -> -r.solved())
                .thenComparingInt(ContestStandingsDto.StandingsRow::penalty)
                .thenComparing(ContestStandingsDto.StandingsRow::username));

        List<ContestStandingsDto.StandingsRow> ranked = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            if (i > 0) {
                var prev = rows.get(i - 1);
                if (r.solved() != prev.solved() || r.penalty() != prev.penalty()) rank = i + 1;
            }
            ranked.add(new ContestStandingsDto.StandingsRow(
                    rank, r.userId(), r.username(), r.solved(), r.penalty(), r.cells()));
        }

        return new ContestStandingsDto(contestId, contest.getTitle(), headers, ranked);
    }

    private void validateInterval(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException("Contest end time must be after start time");
        }
    }

    private Contest findOrThrow(Long id) {
        return contestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contest with id=" + id + " not found"));
    }

    private List<Problem> resolveProblems(List<Long> ids, Long requesterId) {
        if (ids == null) return new ArrayList<>();
        boolean admin = userRepository.findById(requesterId).map(u -> u.getRole() == Role.ADMIN).orElse(false);
        return ids.stream().distinct()
                .map(pid -> {
                    var problem = problemRepository.findById(pid).orElseThrow(() -> new EntityNotFoundException("Problem not found: " + pid));
                    if (!admin && !problem.isVisible() && !problem.getAuthor().getId().equals(requesterId)) {
                        throw new BusinessException("Нельзя включить чужую неопубликованную задачу в контест");
                    }
                    return problem;
                })
                .collect(Collectors.toList());
    }

    private ContestStatus effectiveStatus(Contest c) {
        var now = java.time.LocalDateTime.now();
        return now.isBefore(c.getStartTime()) ? ContestStatus.UPCOMING : now.isBefore(c.getEndTime()) ? ContestStatus.RUNNING : ContestStatus.FINISHED;
    }

    private ContestShortDto toShortDto(Contest c, long participants) {
        return new ContestShortDto(c.getId(), c.getTitle(), c.getStartTime(), c.getEndTime(),
                effectiveStatus(c), c.getProblems().size(), participants);
    }

    private ContestDetailDto toDetailDto(Contest c, boolean registered, Long userId) {
        List<Long> problemIds = c.getProblems().stream().map(Problem::getId).toList();
        Map<Long, Long> solvedMap = new HashMap<>();
        if (!problemIds.isEmpty()) {
            submissionRepository.countDistinctUsersByProblemIdsAndVerdict(problemIds, Verdict.AC)
                    .forEach(row -> solvedMap.put((Long) row[0], (Long) row[1]));
        }

        List<ProblemShortDto> problems = c.getProblems().stream()
                .map(p -> {
                    long solved = solvedMap.getOrDefault(p.getId(), 0L);
                    Set<TagDto> tags = p.getTags().stream()
                            .map(t -> new TagDto(t.getId(), t.getName()))
                            .collect(Collectors.toSet());
                    return new ProblemShortDto(p.getId(), p.getTitle(), p.getDifficulty(), tags, solved, null);
                }).toList();

        // Only expose inviteToken to the organizer
        String inviteToken = null;
        if (userId != null && c.getOrganizer().getId().equals(userId)) {
            inviteToken = c.getInviteToken();
        }

        boolean author = c.getOrganizer().getId().equals(userId);
        boolean admin = userId != null && userRepository.findById(userId).map(u -> u.getRole() == Role.ADMIN).orElse(false);
        if (!author && !admin && (java.time.LocalDateTime.now().isBefore(c.getStartTime()) || (!c.isPublic() && !registered))) problems = List.of();
        var now = java.time.LocalDateTime.now();
        var actualStatus = now.isBefore(c.getStartTime()) ? ContestStatus.UPCOMING : now.isBefore(c.getEndTime()) ? ContestStatus.RUNNING : ContestStatus.FINISHED;
        return new ContestDetailDto(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), actualStatus, problems, registered, c.isPublic(),
                inviteToken, c.getOrganizer().getId());
    }
}
