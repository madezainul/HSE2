package ahqpck.hse.safety.controller.rest;

import ahqpck.hse.safety.model.entity.Notification;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.service.NotificationService;
import ahqpck.hse.safety.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    /**
     * SSE stream endpoint – the browser connects once and receives push events.
     * URL: GET /api/notifications/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        String userId = resolveUserId(authentication);
        if (userId == null) {
            // Return a completed emitter immediately for unauthenticated requests
            SseEmitter empty = new SseEmitter(0L);
            empty.complete();
            return empty;
        }
        return sseEmitterService.subscribe(userId);
    }

    /**
     * Get all notifications for the current user.
     * URL: GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getNotifications(Authentication authentication) {
        String userId = resolveUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).build();

        List<Notification> notifications = notificationService.getForUser(userId);
        List<Map<String, Object>> payload = notifications.stream()
                .map(notificationService::buildPayload)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    /**
     * Get unread count for the current user.
     * URL: GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String userId = resolveUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).build();

        long count = notificationService.getUnreadCount(userId);
        Map<String, Long> resp = new HashMap<>();
        resp.put("count", count);
        return ResponseEntity.ok(resp);
    }

    /**
     * Mark a single notification as read.
     * URL: PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id, Authentication authentication) {
        String userId = resolveUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).build();

        boolean updated = notificationService.markAsRead(id, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", updated);
        return ResponseEntity.ok(resp);
    }

    /**
     * Mark all notifications as read for the current user.
     * URL: PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(Authentication authentication) {
        String userId = resolveUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).build();

        int count = notificationService.markAllRead(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("updated", count);
        return ResponseEntity.ok(resp);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
