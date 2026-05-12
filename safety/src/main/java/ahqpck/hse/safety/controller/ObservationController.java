package ahqpck.hse.safety.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ObservationComment;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.service.ObservationService;
import ahqpck.hse.safety.service.UserService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/observation")
@Slf4j
public class ObservationController {

    @Autowired
    private ObservationRepository observationRepository;
    
    @Autowired
    private ObservationService observationService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    public String index(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {
        
        // Create pageable with default sort by observationDate DESC
        Pageable pageable = PageRequest.of(page, size, Sort.by("observationDate").descending());
        Page<Observation> observationsPage = observationRepository.findAll(pageable);
        
        model.addAttribute("observationsPage", observationsPage);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", "observationDate");
        model.addAttribute("asc", false);
        
        return "observation/index";
    }

    @GetMapping("/{observationCode}")
    @Transactional(readOnly = true)
    public String viewObservation(@PathVariable String observationCode, Model model) {
        Observation observation = observationRepository.findByCode(observationCode);
        if (observation == null) {
            return "redirect:/observation";
        }

        model.addAttribute("observation", observation);
        model.addAttribute("title", "Observation Report");
        model.addAttribute("pageTitle", "Observation Details");

        return "observation/detail";
    }

    /**
     * Add a comment to an observation.
     * @param observationCode the observation code
     * @param content the comment content
     * @param authentication the current user authentication
     * @return JSON response with success status
     */
    @PostMapping("/{observationCode}/comments")
    @ResponseBody
    public Map<String, Object> addComment(
            @PathVariable String observationCode,
            @RequestParam("content") String content,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            Observation observation = observationRepository.findByCode(observationCode);
            if (observation == null) {
                response.put("success", false);
                response.put("message", "Observation not found");
                return response;
            }

            String currentUserId = getCurrentUserId(authentication);
            
            // Fetch user to get full name
            User currentUser = userService.findByUsername(currentUserId);
            String userFullName = currentUser != null ? currentUser.getFullName() : currentUserId;
            
            // Create and save comment
            ObservationComment comment = new ObservationComment();
            comment.setObservation(observation);
            comment.setContent(content);
            comment.setCreatedByUser(currentUser);
            comment.setCreatedAt(LocalDateTime.now());
            
            observationService.addComment(comment);
            
            // Format response
            response.put("success", true);
            response.put("content", content);
            response.put("createdBy", userFullName);
            response.put("formattedDate", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy HH:mm")));
            
            log.info("Comment added to observation: {}", observationCode);
            return response;
        } catch (Exception e) {
            log.error("Error adding comment: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
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
