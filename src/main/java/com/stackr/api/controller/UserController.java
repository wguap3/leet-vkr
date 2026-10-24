package com.stackr.api.controller;

import com.stackr.api.dto.request.UpdateProfileRequest;
import com.stackr.api.dto.response.TagProgressDto;
import com.stackr.api.dto.response.UserDto;
import com.stackr.api.dto.response.UserProfileDto;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.security.UserPrincipal;
import com.stackr.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @GetMapping("/me/progress")
    public ResponseEntity<java.util.List<TagProgressDto>> getMyProgress(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getTopicProgress(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(
            @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<Page<UserProfileDto>> getLeaderboard(Pageable pageable) {
        return ResponseEntity.ok(userService.getLeaderboard(pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        userService.changeRole(id, role);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        userService.setActive(id, active);
        return ResponseEntity.ok().build();
    }
}
