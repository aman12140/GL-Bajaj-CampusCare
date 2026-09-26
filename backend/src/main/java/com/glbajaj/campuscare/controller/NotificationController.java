package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AnalyticsDtos.NotificationDto;
import com.glbajaj.campuscare.dto.CommonDtos.MessageResponse;
import com.glbajaj.campuscare.security.CurrentUser;
import com.glbajaj.campuscare.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationDto> list() { return notificationService.list(currentUser.require()); }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() { return Map.of("count", notificationService.unreadCount(currentUser.require())); }

    @PutMapping("/{id}/read")
    public MessageResponse read(@PathVariable Long id) {
        notificationService.markRead(currentUser.require(), id);
        return new MessageResponse("Marked as read");
    }

    @PutMapping("/read-all")
    public MessageResponse readAll() {
        notificationService.markAllRead(currentUser.require());
        return new MessageResponse("All notifications marked as read");
    }
}
