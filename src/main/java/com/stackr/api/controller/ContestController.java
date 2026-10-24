package com.stackr.api.controller;

import com.stackr.api.dto.request.CreateContestRequest;
import com.stackr.api.dto.request.UpdateContestRequest;
import com.stackr.api.dto.response.*;
import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.security.UserPrincipal;
import com.stackr.api.service.ContestService;
import com.stackr.api.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<List<ContestShortDto>> getAll(
            @RequestParam(required = false) ContestStatus status) {
        return ResponseEntity.ok(contestService.getContests(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContestDetailDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contestService.getContestById(id, principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ContestDetailDto> create(
            @Valid @RequestBody CreateContestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contestService.createContest(req, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ContestDetailDto> update(
            @PathVariable Long id,
            @RequestBody UpdateContestRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contestService.updateContest(id, req, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contestService.deleteContest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasRole('CONTESTANT')")
    public ResponseEntity<Void> register(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        contestService.registerParticipant(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getLeaderboard(id));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ContestAnalyticsDto> getAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getAnalytics(id));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<SubmissionDto>> getSubmissions(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(submissionService.getByProblem(id, pageable));
    }
}
