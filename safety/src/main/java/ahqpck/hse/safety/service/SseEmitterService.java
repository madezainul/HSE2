package ahqpck.hse.safety.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Server-Sent Event (SSE) emitters keyed by userId.
 * Each user can have multiple active browser tabs, hence the List of emitters.
 */
@Slf4j
@Service
public class SseEmitterService {

    // userId -> list of active emitters (one per browser tab)
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Subscribe a new SSE connection for the given user.
     * Timeout is set to 5 minutes; the client will reconnect automatically.
     */
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout

        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);

        // Clean up on completion / timeout / error
        Runnable remove = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> removeEmitter(userId, emitter));

        // Send a keep-alive comment immediately so the connection is confirmed
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        log.debug("SSE subscribed for user {}", userId);
        return emitter;
    }

    /**
     * Send a named event with JSON payload to all emitters of the given user.
     */
    public void sendToUser(String userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    /**
     * Send a named event to every connected user EXCEPT the specified one.
     */
    public void sendToAllExcept(String excludeUserId, String eventName, Object data) {
        for (String userId : emitters.keySet()) {
            if (!userId.equals(excludeUserId)) {
                sendToUser(userId, eventName, data);
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
