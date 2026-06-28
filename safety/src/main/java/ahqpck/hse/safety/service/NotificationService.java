package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.Notification;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.NotificationRepository;
import ahqpck.hse.safety.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles creating, fetching, and marking notifications.
 * When an Incident or Observation is created, a notification is persisted for
 * every user except the creator, and a live SSE push is sent to all online users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;

    /**
     * Create a notification for every user except the creator, then push via SSE.
     *
     * @param type            "INCIDENT" or "OBSERVATION"
     * @param referenceCode   e.g. "INC-000001"
     * @param createdByUserId the user who created the record (excluded from notification)
     * @param creatorFullName display name of the creator
     */
    @Transactional
    public void notifyAllExcept(String type, String referenceCode,
                                String createdByUserId, String creatorFullName) {
        String label;
        String linkUrl;
        switch (type) {
            case "INCIDENT":
                label = "Incident";
                linkUrl = "/incident/" + referenceCode;
                break;
            case "TOOLBOX_MEETING":
                label = "Toolbox Meeting";
                linkUrl = "/training/toolbox/" + referenceCode;
                break;
            default:
                label = "Observation";
                linkUrl = "/observation/" + referenceCode;
                break;
        }
        String message = "New " + label + " " + referenceCode + " reported by " + creatorFullName;

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (user.getId().equals(createdByUserId)) continue; // skip the creator

            Notification notification = Notification.builder()
                    .forUserId(user.getId())
                    .createdByUserId(createdByUserId)
                    .message(message)
                    .type(type)
                    .referenceCode(referenceCode)
                    .linkUrl(linkUrl)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepo.save(notification);

            // Push live event to online users
            Map<String, Object> payload = buildPayload(notification);
            sseEmitterService.sendToUser(user.getId(), "notification", payload);
        }

        log.info("Notifications created for {} ({}) by user {}", type, referenceCode, createdByUserId);
    }

    /**
     * Get all notifications for a user (most recent first).
     */
    public List<Notification> getForUser(String userId) {
        return notificationRepo.findByForUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Count unread notifications for a user.
     */
    public long getUnreadCount(String userId) {
        return notificationRepo.countByForUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a single notification as read (only if it belongs to the requesting user).
     */
    @Transactional
    public boolean markAsRead(Long notificationId, String userId) {
        return notificationRepo.findById(notificationId)
                .filter(n -> n.getForUserId().equals(userId))
                .map(n -> {
                    n.setIsRead(true);
                    notificationRepo.save(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Mark all notifications for a user as read.
     */
    @Transactional
    public int markAllRead(String userId) {
        return notificationRepo.markAllReadByUserId(userId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    public Map<String, Object> buildPayload(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("message", n.getMessage());
        map.put("type", n.getType());
        map.put("referenceCode", n.getReferenceCode());
        map.put("linkUrl", n.getLinkUrl());
        map.put("isRead", n.getIsRead());
        map.put("createdAt", n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        return map;
    }
}
