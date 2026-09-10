package com.stackr.api.dto.response;

import com.stackr.api.entity.enums.Role;

import java.time.LocalDateTime;

public record UserProfileDto(
        Long id,
        String username,
        String email,
        Role role,
        Integer rating,
        String ratingTier,
        String ratingTierIcon,
        String avatarUrl,
        LocalDateTime registeredAt,
        long totalAC,
        long totalSubmissions
) {
    /** Derive tier from rating value. */
    public static String tierName(int rating) {
        if (rating >= 2000) return "Мастер";
        if (rating >= 1500) return "Эксперт";
        if (rating >= 1000) return "Продвинутый";
        if (rating >= 500)  return "Участник";
        return "Новичок";
    }

    public static String tierIcon(int rating) {
        if (rating >= 2000) return "👑";
        if (rating >= 1500) return "🔥";
        if (rating >= 1000) return "💫";
        if (rating >= 500)  return "⚡";
        return "🔰";
    }

    public static String tierColor(int rating) {
        if (rating >= 2000) return "#f59e0b";
        if (rating >= 1500) return "#8b5cf6";
        if (rating >= 1000) return "#3b82f6";
        if (rating >= 500)  return "#22c55e";
        return "#71717a";
    }
}
