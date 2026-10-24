package com.stackr.api.controller;

import com.stackr.api.dto.request.RunCodeRequest;
import com.stackr.api.dto.request.SubmitRequest;
import com.stackr.api.dto.response.RunCodeResult;
import com.stackr.api.dto.response.SubmissionDto;
import com.stackr.api.security.UserPrincipal;
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

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("hasRole('CONTESTANT') or hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<SubmissionDto> submit(
            @Valid @RequestBody SubmitRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(submissionService.submit(req, principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isOrganizer = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(submissionService.getSubmission(id, principal.getId(), isOrganizer));
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('CONTESTANT') or hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<RunCodeResult> runCode(
            @Valid @RequestBody RunCodeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(submissionService.runCode(req));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<SubmissionDto>> getMy(
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) Long contestId,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        return ResponseEntity.ok(submissionService.getMySubmissions(principal.getId(), problemId, contestId, pageable));
    }

    @GetMapping("/problem/{problemId}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<SubmissionDto>> getByProblem(
            @PathVariable Long problemId,
            Pageable pageable) {
        return ResponseEntity.ok(submissionService.getByProblem(problemId, pageable));
    }
}
