package com.stackr.api.service;

import com.stackr.api.dto.response.NotificationDto;
import com.stackr.api.entity.Notification;
import com.stackr.api.entity.User;
import com.stackr.api.repository.NotificationRepository;
import com.stackr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void create(Long userId, String message, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot create notification: user {} not found", userId);
            return;
        }
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUnread(Long userId) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void markAllRead(Long userId) {
        int updated = notificationRepository.markAllReadByUserId(userId);
        log.debug("Marked {} notifications as read for user {}", updated, userId);
    }
}
