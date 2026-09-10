package com.stackr.api.controller;

import com.stackr.api.dto.request.CreateTestCaseRequest;
import com.stackr.api.dto.request.UpdateTestCaseRequest;
import com.stackr.api.dto.response.TestCaseDto;
import com.stackr.api.security.UserPrincipal;
import com.stackr.api.service.TestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/testcases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping
    public ResponseEntity<List<TestCaseDto>> getAll(
            @PathVariable Long problemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean showAll = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(testCaseService.getTestCases(problemId, showAll));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestCaseDto> create(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateTestCaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testCaseService.create(problemId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestCaseDto> update(
            @PathVariable Long problemId,
            @PathVariable Long id,
            @RequestBody UpdateTestCaseRequest req) {
        return ResponseEntity.ok(testCaseService.update(problemId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long problemId,
            @PathVariable Long id) {
        testCaseService.delete(problemId, id);
        return ResponseEntity.noContent().build();
    }
}
