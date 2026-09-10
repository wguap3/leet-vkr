package com.stackr.api.controller;

import com.stackr.api.dto.request.CreateProblemRequest;
import com.stackr.api.dto.request.UpdateProblemRequest;
import com.stackr.api.dto.response.ProblemDetailDto;
import com.stackr.api.dto.response.ProblemShortDto;
import com.stackr.api.entity.enums.Difficulty;
import com.stackr.api.security.UserPrincipal;
import com.stackr.api.service.ProblemService;
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
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<Page<ProblemShortDto>> getAll(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) Long tagId,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        return ResponseEntity.ok(problemService.getProblems(difficulty, tagId, principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProblemDetailDto> create(
            @Valid @RequestBody CreateProblemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(problemService.createProblem(req, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProblemDetailDto> update(
            @PathVariable Long id,
            @RequestBody UpdateProblemRequest req) {
        return ResponseEntity.ok(problemService.updateProblem(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        problemService.deleteProblem(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setVisibility(
            @PathVariable Long id,
            @RequestParam boolean visible) {
        problemService.setVisibility(id, visible);
        return ResponseEntity.ok().build();
    }
}
