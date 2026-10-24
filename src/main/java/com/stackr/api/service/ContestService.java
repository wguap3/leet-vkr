package com.stackr.api.service;

import com.stackr.api.dto.request.CreateContestRequest;
import com.stackr.api.dto.request.UpdateContestRequest;
import com.stackr.api.dto.response.*;
import com.stackr.api.entity.Contest;
import com.stackr.api.entity.ContestParticipation;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.ConflictException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
        return contests.stream().map(this::toShortDto).toList();
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

    private ContestShortDto toShortDto(Contest c) {
        long participants = contestRepository.countParticipants(c.getId());
        return new ContestShortDto(c.getId(), c.getTitle(), c.getStartTime(), c.getEndTime(),
                c.getStatus(), c.getProblems().size(), participants);
    }

    private ContestDetailDto toDetailDto(Contest c, boolean registered, Long userId) {
        List<ProblemShortDto> problems = c.getProblems().stream()
                .map(p -> {
                    long solved = submissionRepository.countDistinctUsersByProblemIdAndVerdict(p.getId(), Verdict.AC);
                    Set<TagDto> tags = p.getTags().stream()
                            .map(t -> new TagDto(t.getId(), t.getName()))
                            .collect(Collectors.toSet());
                    return new ProblemShortDto(p.getId(), p.getTitle(), p.getDifficulty(), tags, solved, null);
                }).toList();
        return new ContestDetailDto(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.getStatus(), problems, registered);
    }
}
