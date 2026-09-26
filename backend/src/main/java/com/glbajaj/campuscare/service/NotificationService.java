package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AnalyticsDtos.NotificationDto;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.NotificationRepository;
import com.glbajaj.campuscare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Database-backed notifications (no WebSockets: the navbar bell polls the unread count). */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notify(User user, Issue issue, String title, String message) {
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setIssue(issue);
        n.setTitle(title);
        n.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
        notificationRepository.save(n);
    }

    @Transactional
    public void notifyAdmins(Issue issue, String title, String message) {
        for (User admin : userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)) {
            notify(admin, issue, title, message);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(User user) {
        return notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(n -> new NotificationDto(n.getId(), n.getIssue() == null ? null : n.getIssue().getId(),
                        n.getIssue() == null ? null : n.getIssue().getIssueNumber(),
                        n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) { return notificationRepository.countByUserIdAndReadFalse(user.getId()); }

    @Transactional
    public void markRead(User user, Long id) {
        Notification n = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public void markAllRead(User user) { notificationRepository.markAllRead(user.getId()); }
}
