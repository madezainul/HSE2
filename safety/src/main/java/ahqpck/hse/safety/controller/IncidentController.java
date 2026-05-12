package ahqpck.hse.safety.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.IncidentComment;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.service.IncidentService;
import ahqpck.hse.safety.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/incident")
@Slf4j
public class IncidentController {
    
    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String index(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {
        
        // Create pageable with default sort by reportDate DESC
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
        Page<Incident> incidentsPage = incidentRepository.findAll(pageable);
        
        model.addAttribute("incidentsPage", incidentsPage);
        model.addAttribute("incident", new Incident());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", "reportDate");
        model.addAttribute("asc", false);
        
        return "incident/index";
    }

    @PostMapping
    @Transactional
    public String createIncident(
            @RequestParam String reportDate,
            @RequestParam String areaCode,
            @RequestParam String severity,
            @RequestParam String status,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam(required = false) String involvedPersonName,
            @RequestParam(required = false) String involvedPersonEmployeeId,
            @RequestParam(required = false) String involvedPersonPosition,
            @RequestParam(required = false) String witnesses,
            @RequestParam(required = false) String witnessesEmployeeId,
            @RequestParam(required = false) String immediateAction,
            @RequestParam(required = false) String correctiveAction,
            @RequestParam(required = false) Boolean medicalAttentionRequired,
            @RequestParam(value = "incidentImages", required = false) MultipartFile[] incidentImages,
            @RequestParam(value = "incidentDocuments", required = false) MultipartFile[] incidentDocuments,
            Authentication authentication) {
        try {
            // Build IncidentDTO from request parameters
            IncidentDTO dto = new IncidentDTO();

            // Parse reportDate from datetime-local format
            LocalDateTime parseDate = LocalDateTime.parse(reportDate, 
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.setReportDate(parseDate);

            User currentUser = userService.findByUsername(getCurrentUserId(authentication));
            String currentUserId = currentUser.getId();
            dto.setReportedBy(currentUser.getUsername());
            
            dto.setSeverity(severity);
            dto.setStatus(status);
            dto.setType(type);
            dto.setDescription(description);
            dto.setInvolvedPersonName(involvedPersonName);
            dto.setInvolvedPersonEmployeeId(involvedPersonEmployeeId);
            dto.setInvolvedPersonPosition(involvedPersonPosition);
            dto.setWitnesses(witnesses);
            dto.setWitnessesEmployeeId(witnessesEmployeeId);
            dto.setImmediateAction(immediateAction);
            dto.setCorrectiveAction(correctiveAction);
            dto.setMedicalAttentionRequired(medicalAttentionRequired);

            // Set area
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setCode(areaCode);
            dto.setArea(areaDTO);

            // Save Incident using service with current user ID
            Incident incident = incidentService.save(dto, currentUserId);

            // Upload images if provided
            if (incidentImages != null && incidentImages.length > 0) {
                for (MultipartFile imageFile : incidentImages) {
                    if (!imageFile.isEmpty()) {
                        incidentService.saveFile(incident.getCode(), imageFile, "IMAGE");
                    }
                }
            }

            // Upload documents if provided
            if (incidentDocuments != null && incidentDocuments.length > 0) {
                for (MultipartFile docFile : incidentDocuments) {
                    if (!docFile.isEmpty()) {
                        incidentService.saveFile(incident.getCode(), docFile, "DOCUMENT");
                    }
                }
            }
            
            log.info("New incident created: {}", incident.getCode());
            
            return "redirect:/incident/" + incident.getCode();
        } catch (Exception e) {
            log.error("Error creating incident: {}", e.getMessage());
            return "redirect:/incident?error=true";
        }
    }

    @GetMapping("/{incidentCode}")
    @Transactional(readOnly = true)
    public String viewIncident(@PathVariable String incidentCode, Model model) {
        Incident incident = incidentRepository.findByCodeWithFiles(incidentCode);
        if (incident == null) {
            return "redirect:/incident";
        }
        model.addAttribute("incident", incident);
        model.addAttribute("title", "Incident Report");
        model.addAttribute("pageTitle", "Incident Details");

        return "incident/detail";
    }

    /**
     * Add a comment to an observation.
     * @param observationCode the observation code
     * @param content the comment content
     * @param authentication the current user authentication
     * @return JSON response with success status
     */
    @PostMapping("/{incidentCode}/comments")
    @ResponseBody
    public Map<String, Object> addComment(
            @PathVariable String incidentCode,
            @RequestParam("content") String content,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            Incident incident = incidentRepository.findByCode(incidentCode);
            if (incident == null) {
                response.put("success", false);
                response.put("message", "Incident not found");
                return response;
            }

            String currentUserId = getCurrentUserId(authentication);

            // Fetch user to get full name
            User currentUser = userService.findByUsername(currentUserId);
            String userFullName = currentUser != null ? currentUser.getFullName() : currentUserId;

            // Create and save comment
            IncidentComment comment = new IncidentComment();
            comment.setIncident(incident);
            comment.setContent(content);
            comment.setCreatedByUser(currentUser);
            comment.setCreatedAt(LocalDateTime.now());

            incidentService.addComment(comment);

            // Format response
            response.put("success", true);
            response.put("content", content);
            response.put("createdBy", userFullName);
            response.put("formattedDate", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy HH:mm")));

            log.info("Comment added to Incident: {}", incidentCode);
            return response;
        } catch (Exception e) {
            log.error("Error adding comment to incident {}: {}", incidentCode, e.getMessage());
            response.put("success", false);
            response.put("message", "Error adding comment: " + e.getMessage());
            return response;
        }
    }

    /**
     * Get current user ID from authentication
     */
    private String getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }

}
