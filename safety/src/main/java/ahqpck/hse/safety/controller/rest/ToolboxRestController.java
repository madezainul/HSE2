package ahqpck.hse.safety.controller.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.dto.ToolboxMeetingDTO;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.service.NotificationService;
import ahqpck.hse.safety.service.ToolboxMeetingService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@RestController
@RequestMapping("/api/toolbox")
public class ToolboxRestController {

    @Autowired
    private ToolboxMeetingService toolboxService;

    @Autowired
    private ToolboxMeetingRepository toolboxRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createToolboxMeeting(
            @RequestParam String location,
            @RequestParam String meetingDate,
            @RequestParam String timeStarted,
            @RequestParam String status,
            @RequestParam(required = false) String timeEnd,
            @RequestParam(required = false) String supervisorEmployeeId,
            @RequestParam(required = false) String supervisorName,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "toolboxImages", required = false) MultipartFile[] toolboxImages,
            @RequestParam(value = "toolboxDocuments", required = false) MultipartFile[] toolboxDocuments,
            Authentication authentication) {
        try {
            ToolboxMeetingDTO dto = new ToolboxMeetingDTO();
            dto.setLocation(location);
            dto.setMeetingDate(LocalDate.parse(meetingDate));
            dto.setTimeStarted(LocalTime.parse(timeStarted));
            dto.setStatus(status);
            if (timeEnd != null && !timeEnd.isBlank()) dto.setTimeEnd(LocalTime.parse(timeEnd));
            dto.setSupervisorEmployeeId(supervisorEmployeeId);
            dto.setSupervisor(supervisorName);
            dto.setNotes(notes);

            ToolboxMeeting meeting = toolboxService.save(dto);

            if (toolboxImages != null) {
                for (MultipartFile img : toolboxImages) {
                    if (!img.isEmpty()) {
                        toolboxService.saveFile(meeting.getCode(), img, "IMAGE");
                    }
                }
            }
            if (toolboxDocuments != null) {
                for (MultipartFile doc : toolboxDocuments) {
                    if (!doc.isEmpty()) {
                        toolboxService.saveFile(meeting.getCode(), doc, "DOCUMENT");
                    }
                }
            }

            // Notify other users
            String currentUserId = null;
            String currentUserFullName = "Unknown";
            if (authentication != null && authentication.getPrincipal() instanceof User principal) {
                currentUserId = principal.getId();
                currentUserFullName = principal.getFullName() != null ? principal.getFullName() : principal.getUsername();
            } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                var cu = userService.findByUsername(ud.getUsername());
                if (cu != null) {
                    currentUserId = cu.getId();
                    currentUserFullName = cu.getFullName() != null ? cu.getFullName() : cu.getUsername();
                }
            }
            if (currentUserId != null) {
                notificationService.notifyAllExcept("TOOLBOX_MEETING", meeting.getCode(), currentUserId, currentUserFullName);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Toolbox meeting saved successfully");
            response.put("code", meeting.getCode());
            return ResponseEntity.status(201).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error saving toolbox meeting: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error saving toolbox meeting: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while saving the toolbox meeting");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{meetingCode}")
    public ResponseEntity<?> getToolboxMeeting(@PathVariable String meetingCode) {
        try {
            ToolboxMeeting meeting = toolboxService.findByCode(meetingCode);
            if (meeting == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Toolbox meeting not found");
                return ResponseEntity.status(404).body(error);
            }
            return ResponseEntity.ok(ToolboxMeetingDTO.fromEntity(meeting));
        } catch (Exception e) {
            log.error("Error retrieving toolbox meeting: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while retrieving the toolbox meeting");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping(value = "/{meetingCode}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateToolboxMeeting(
            @PathVariable String meetingCode,
            @RequestParam String location,
            @RequestParam String meetingDate,
            @RequestParam String timeStarted,
            @RequestParam String status,
            @RequestParam(required = false) String timeEnd,
            @RequestParam(required = false) String supervisorEmployeeId,
            @RequestParam(required = false) String supervisorName,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "toolboxImages", required = false) MultipartFile[] toolboxImages,
            @RequestParam(value = "toolboxDocuments", required = false) MultipartFile[] toolboxDocuments) {
        try {
            ToolboxMeetingDTO dto = new ToolboxMeetingDTO();
            dto.setLocation(location);
            dto.setMeetingDate(LocalDate.parse(meetingDate));
            dto.setTimeStarted(LocalTime.parse(timeStarted));
            dto.setStatus(status);
            if (timeEnd != null && !timeEnd.isBlank()) dto.setTimeEnd(LocalTime.parse(timeEnd));
            dto.setSupervisorEmployeeId(supervisorEmployeeId);
            dto.setSupervisor(supervisorName);
            dto.setNotes(notes);

            ToolboxMeeting meeting = toolboxService.update(meetingCode, dto);

            if (toolboxImages != null) {
                for (MultipartFile img : toolboxImages) {
                    if (!img.isEmpty()) {
                        toolboxService.saveFile(meeting.getCode(), img, "IMAGE");
                    }
                }
            }
            if (toolboxDocuments != null) {
                for (MultipartFile doc : toolboxDocuments) {
                    if (!doc.isEmpty()) {
                        toolboxService.saveFile(meeting.getCode(), doc, "DOCUMENT");
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Toolbox meeting updated successfully");
            response.put("code", meeting.getCode());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating toolbox meeting {}: {}", meetingCode, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error updating toolbox meeting {}: {}", meetingCode, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while updating the toolbox meeting");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{meetingCode}")
    public ResponseEntity<?> deleteToolboxMeeting(@PathVariable String meetingCode) {
        try {
            ToolboxMeeting meeting = toolboxService.findByCode(meetingCode);
            if (meeting == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Toolbox meeting not found");
                return ResponseEntity.status(404).body(error);
            }
            toolboxRepository.delete(meeting);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Toolbox meeting deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting toolbox meeting {}: {}", meetingCode, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while deleting the toolbox meeting");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
