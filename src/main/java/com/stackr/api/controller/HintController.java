package com.stackr.api.controller;

import com.stackr.api.dto.request.CreateHintRequest;
import com.stackr.api.dto.response.HintDto;
import com.stackr.api.security.UserPrincipal;
import com.stackr.api.service.HintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/hints")
@RequiredArgsConstructor
public class HintController {

    private final HintService hintService;

    /** Returns hints for the current user — locked ones have text=null */
    @GetMapping
    public ResponseEntity<List<HintDto>> getHints(
            @PathVariable Long problemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<HintDto> hints = isAdmin
                ? hintService.getHintsAdmin(problemId)
                : hintService.getHintsForUser(problemId, principal.getId());
        return ResponseEntity.ok(hints);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    public ResponseEntity<HintDto> create(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateHintRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hintService.createHint(problemId, req));
    }

    @DeleteMapping("/{hintId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long problemId,
            @PathVariable Long hintId) {
        hintService.deleteHint(hintId);
        return ResponseEntity.noContent().build();
    }
}
