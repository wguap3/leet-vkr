package com.stackr.api.service;

import com.stackr.api.dto.request.UpdateProfileRequest;
import com.stackr.api.dto.response.TagProgressDto;
import com.stackr.api.dto.response.UserDto;
import com.stackr.api.dto.response.UserProfileDto;
import com.stackr.api.entity.enums.Role;
import com.stackr.api.entity.enums.Verdict;
import com.stackr.api.dto.request.ChangePasswordRequest;
import com.stackr.api.exception.BusinessException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.SubmissionRepository;
import com.stackr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        var user = findOrThrow(userId);
        long totalAC = submissionRepository.countDistinctSolvedByUserId(userId, Verdict.AC);
        long totalSubs = submissionRepository.countByUserId(userId);
        int rating = user.getRating() == null ? 0 : user.getRating();
        return new UserProfileDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                rating, UserProfileDto.tierName(rating), UserProfileDto.tierIcon(rating),
                user.getAvatarUrl(), user.getRegisteredAt(), totalAC, totalSubs);
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

    private UserProfileDto toProfileDto(com.stackr.api.entity.User u, long totalAC, long totalSubs) {
        int rating = u.getRating() == null ? 0 : u.getRating();
        return new UserProfileDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(),
                rating, UserProfileDto.tierName(rating), UserProfileDto.tierIcon(rating),
                u.getAvatarUrl(), u.getRegisteredAt(), totalAC, totalSubs);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getLeaderboard(Pageable pageable) {
        var page = userRepository.findAllByOrderByRatingDesc(pageable);
        if (page.isEmpty()) return page.map(u -> toProfileDto(u, 0L, 0L));

        java.util.List<Long> ids = page.getContent().stream()
                .map(com.stackr.api.entity.User::getId).toList();

        // Batch: solved AC count per user
        java.util.Map<Long, Long> acMap = new java.util.HashMap<>();
        submissionRepository.countDistinctSolvedByUserIdsAndVerdict(ids, Verdict.AC)
                .forEach(row -> acMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));

        // Batch: total submissions per user
        java.util.Map<Long, Long> subMap = new java.util.HashMap<>();
        submissionRepository.countSubmissionsByUserIds(ids)
                .forEach(row -> subMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));

        return page.map(u -> toProfileDto(u,
                acMap.getOrDefault(u.getId(), 0L),
                subMap.getOrDefault(u.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public java.util.List<TagProgressDto> getTopicProgress(Long userId) {
        var solvedRows = submissionRepository.countSolvedPerTagByUserId(userId);
        var totalRows  = submissionRepository.countProblemsPerTag();

        // Build map tagId → total
        java.util.Map<Long, Long> totalMap = new java.util.HashMap<>();
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        for (Object[] row : totalRows) {
            Long tagId = ((Number) row[0]).longValue();
            totalMap.put(tagId, ((Number) row[2]).longValue());
            nameMap.put(tagId, (String) row[1]);
        }

        // Merge with solved
        java.util.Map<Long, Long> solvedMap = new java.util.HashMap<>();
        for (Object[] row : solvedRows) {
            Long tagId = ((Number) row[0]).longValue();
            solvedMap.put(tagId, ((Number) row[2]).longValue());
            nameMap.putIfAbsent(tagId, (String) row[1]);
        }

        return nameMap.entrySet().stream()
                .map(e -> new TagProgressDto(
                        e.getKey(), e.getValue(),
                        solvedMap.getOrDefault(e.getKey(), 0L),
                        totalMap.getOrDefault(e.getKey(), 0L)))
                .sorted(java.util.Comparator.comparing(TagProgressDto::tagName))
                .toList();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        var user = findOrThrow(userId);
        if (!passwordEncoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Неверный текущий пароль");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
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
