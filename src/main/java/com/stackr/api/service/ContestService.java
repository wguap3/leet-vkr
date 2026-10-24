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
    public List<ContestShortDto> getContests(ContestStatus status) {
        List<Contest> contests = (status != null)
                ? contestRepository.findByStatus(status)
                : contestRepository.findAll();
        if (contests.isEmpty()) return List.of();

        // Batch participant counts — 1 query instead of N
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

    @Transactional
    public ContestDetailDto createContest(CreateContestRequest req, Long organizerId) {
        var organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Problem> problems = resolveProblems(req.problemIds());
        var contest = Contest.builder()
                .title(req.title())
                .description(req.description())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .problems(problems)
                .organizer(organizer)
                .isPublic(req.isPublic() != null ? req.isPublic() : true)
                .build();
        contest = contestRepository.save(contest);
        return toDetailDto(contest, false, organizerId);
    }

    @Transactional
    public ContestDetailDto updateContest(Long id, UpdateContestRequest req, Long userId) {
        var contest = findOrThrow(id);

        // Ownership check: only the organizer or an admin can update this contest
        if (!contest.getOrganizer().getId().equals(userId)) {
            var caller = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            if (caller.getRole() != Role.ADMIN) {
                throw new BusinessException("You are not authorized to update this contest");
            }
        }

        if (req.title() != null) contest.setTitle(req.title());
        if (req.description() != null) contest.setDescription(req.description());
        if (req.startTime() != null) contest.setStartTime(req.startTime());
        if (req.endTime() != null) contest.setEndTime(req.endTime());
        if (req.problemIds() != null) contest.setProblems(resolveProblems(req.problemIds()));
        if (req.isPublic() != null) contest.setPublic(req.isPublic());
        contest = contestRepository.save(contest);
        boolean registered = participationRepository.existsByUserIdAndContestId(userId, id);
        return toDetailDto(contest, registered, userId);
    }

    @Transactional
    public void deleteContest(Long id) {
        if (!contestRepository.existsById(id)) {
            throw new EntityNotFoundException("Contest not found: " + id);
        }
        contestRepository.deleteById(id);
    }

    @Transactional
    public void registerParticipant(Long contestId, Long userId) {
        var contest = findOrThrow(contestId);
        if (contest.getStatus() == ContestStatus.FINISHED) {
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

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(Long contestId) {
        var entries = participationRepository.findLeaderboard(contestId);
        int rank = 1;
        List<LeaderboardEntryDto> result = new ArrayList<>();
        for (var cp : entries) {
            result.add(new LeaderboardEntryDto(
                    rank++,
                    cp.getUser().getId(),
                    cp.getUser().getUsername(),
                    cp.getScore(),
                    cp.getPenalty(),
                    cp.getSubmissionCount()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ContestAnalyticsDto getAnalytics(Long contestId) {
        var contest = findOrThrow(contestId);
        var submissions = submissionRepository.findAllByContestId(contestId);

        // Build problem headers
        List<ContestAnalyticsDto.ProblemHeader> problems = contest.getProblems().stream()
                .map(p -> new ContestAnalyticsDto.ProblemHeader(p.getId(), p.getTitle()))
                .toList();

        // Group submissions by user, then by problem
        Map<Long, String> userNames = new HashMap<>();
        // userId → (problemId → list of submissions)
        Map<Long, Map<Long, List<com.stackr.api.entity.Submission>>> grouped = new HashMap<>();

        for (var s : submissions) {
            Long uid = s.getUser().getId();
            Long pid = s.getProblem().getId();
            userNames.put(uid, s.getUser().getUsername());
            grouped.computeIfAbsent(uid, k -> new HashMap<>())
                   .computeIfAbsent(pid, k -> new ArrayList<>())
                   .add(s);
        }

        List<ContestAnalyticsDto.ParticipantRow> rows = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Long uid = entry.getKey();
            Map<Long, ContestAnalyticsDto.ProblemResult> results = new HashMap<>();
            for (var pe : entry.getValue().entrySet()) {
                Long pid = pe.getKey();
                var subs = pe.getValue();
                int attempts = subs.size();
                boolean solved = subs.stream().anyMatch(s -> s.getVerdict() == com.stackr.api.entity.enums.Verdict.AC);
                Integer firstAcMin = null;
                if (solved) {
                    var firstAc = subs.stream()
                            .filter(s -> s.getVerdict() == com.stackr.api.entity.enums.Verdict.AC)
                            .min(Comparator.comparing(com.stackr.api.entity.Submission::getSubmittedAt))
                            .orElse(null);
                    if (firstAc != null && contest.getStartTime() != null) {
                        firstAcMin = (int) java.time.Duration.between(contest.getStartTime(), firstAc.getSubmittedAt()).toMinutes();
                    }
                }
                results.put(pid, new ContestAnalyticsDto.ProblemResult(attempts, solved, firstAcMin));
            }
            rows.add(new ContestAnalyticsDto.ParticipantRow(uid, userNames.get(uid), results));
        }

        // Sort by solved desc, then username
        rows.sort(Comparator.<ContestAnalyticsDto.ParticipantRow>comparingInt(r -> -r.results().values().stream()
                        .mapToInt(pr -> pr.solved() ? 1 : 0).sum())
                .thenComparing(r -> r.username()));

        return new ContestAnalyticsDto(contestId, contest.getTitle(), problems, rows);
    }


    @Transactional(readOnly = true)
    public ContestStandingsDto getStandings(Long contestId) {
        var contest = findOrThrow(contestId);
        var submissions = submissionRepository.findAllByContestId(contestId);

        // problem order  A, B, C ...
        List<com.stackr.api.entity.Problem> problems = contest.getProblems();
        List<ContestStandingsDto.ProblemHeader> headers = new ArrayList<>();
        for (int i = 0; i < problems.size(); i++) {
            var p = problems.get(i);
            headers.add(new ContestStandingsDto.ProblemHeader(p.getId(), p.getTitle(),
                    String.valueOf((char) ('A' + i))));
        }
        Set<Long> problemIds = problems.stream()
                .map(com.stackr.api.entity.Problem::getId).collect(Collectors.toSet());

        // Collect all relevant userIds: registered participants + anyone who submitted
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

        // userId → (problemId → submissions sorted by time, already ordered by findAllByContestId)
        Map<Long, Map<Long, List<com.stackr.api.entity.Submission>>> grouped = new HashMap<>();
        for (var s : submissions) {
            Long uid = s.getUser().getId();
            Long pid = s.getProblem().getId();
            if (!problemIds.contains(pid)) continue;   // skip off-contest submissions
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
                // subs are already in ascending time order (findAllByContestId ORDER BY submittedAt ASC)

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
                        break; // only count submissions up to first AC
                    } else if (s.getVerdict() != com.stackr.api.entity.enums.Verdict.PENDING) {
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

        // Sort: solved DESC → penalty ASC → username ASC
        rows.sort(Comparator.<ContestStandingsDto.StandingsRow>comparingInt(r -> -r.solved())
                .thenComparingInt(ContestStandingsDto.StandingsRow::penalty)
                .thenComparing(ContestStandingsDto.StandingsRow::username));

        // Assign ranks (same solved+penalty → same rank)
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

    private Contest findOrThrow(Long id) {
        return contestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contest with id=" + id + " not found"));
    }

    private List<Problem> resolveProblems(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(pid -> problemRepository.findById(pid)
                        .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + pid)))
                .collect(Collectors.toList());
    }

    private ContestShortDto toShortDto(Contest c, long participants) {
        return new ContestShortDto(c.getId(), c.getTitle(), c.getStartTime(), c.getEndTime(),
                c.getStatus(), c.getProblems().size(), participants);
    }

    private ContestDetailDto toDetailDto(Contest c, boolean registered, Long userId) {
        // Batch-fetch solved counts for all problems in this contest
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
        return new ContestDetailDto(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.getStatus(), problems, registered, c.isPublic());
    }
}
