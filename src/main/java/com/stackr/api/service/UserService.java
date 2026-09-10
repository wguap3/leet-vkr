package com.stackr.api.service;

import com.stackr.api.dto.request.UpdateProfileRequest;
import com.stackr.api.dto.response.UserDto;
import com.stackr.api.dto.response.UserProfileDto;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.SubmissionRepository;
import com.stackr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        var user = findOrThrow(userId);
        long totalAC = submissionRepository.countDistinctSolvedByUserId(userId, Verdict.AC);
        long totalSubs = submissionRepository.countByUserId(userId);
        return new UserProfileDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getRating(), user.getAvatarUrl(), user.getRegisteredAt(), totalAC, totalSubs);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        var user = findOrThrow(userId);
        user.setAvatarUrl(req.avatarUrl());
        userRepository.save(user);
        return getProfile(userId);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(u ->
                new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getRating(), u.getRegisteredAt()));
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getLeaderboard(Pageable pageable) {
        return userRepository.findAllByOrderByRatingDesc(pageable)
                .map(u -> {
                    long totalAC = submissionRepository.countDistinctSolvedByUserId(u.getId(), Verdict.AC);
                    long totalSubs = submissionRepository.countByUserId(u.getId());
                    return new UserProfileDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(),
                            u.getRating(), u.getAvatarUrl(), u.getRegisteredAt(), totalAC, totalSubs);
                });
    }

    @Transactional
    public void changeRole(Long userId, Role role) {
        var user = findOrThrow(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        var user = findOrThrow(userId);
        user.setActive(active);
        userRepository.save(user);
    }

    private com.stackr.api.entity.User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
