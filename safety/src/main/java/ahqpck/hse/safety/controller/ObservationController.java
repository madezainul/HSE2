package ahqpck.hse.safety.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import ahqpck.hse.safety.model.dto.ObservationSearchCriteria;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ObservationComment;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.service.AreaService;
import ahqpck.hse.safety.service.ObservationExportService;
import ahqpck.hse.safety.service.ObservationService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.util.ListQuerySupport;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
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

    @Autowired
    private ObservationExportService observationExportService;

    @Autowired
    private AreaService areaService;

    @GetMapping
    public String index(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String observationDateFrom,
            @RequestParam(defaultValue = "") String observationDateTo,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String mitigation,
            @RequestParam(defaultValue = "") String areaCode,
            @RequestParam(defaultValue = "observationDate") String sortBy,
            @RequestParam(defaultValue = "false") String asc,
            @RequestParam(defaultValue = "") String hiddenColumns,
            Model model) {

        ObservationSearchCriteria criteria = ObservationSearchCriteria.of(
                keyword, observationDateFrom, observationDateTo, category, type, status, mitigation, areaCode);
        boolean ascending = "true".equalsIgnoreCase(asc);

        Page<Observation> observationsPage = observationService.search(criteria, page, size, sortBy, ascending);

        model.addAttribute("observationsPage", observationsPage);
        model.addAttribute("pageSize", ListQuerySupport.safeSize(size));
        model.addAttribute("title", "Observation Report");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("asc", ascending);

        // Echoed back so the filter form keeps its values and the page links keep the filters
        model.addAttribute("keyword", keyword);
        model.addAttribute("observationDateFrom", observationDateFrom);
        model.addAttribute("observationDateTo", observationDateTo);
        model.addAttribute("category", category);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("mitigation", mitigation);
        model.addAttribute("areaCode", areaCode);
        model.addAttribute("hiddenColumns", hiddenColumns);

        // Choices for the filter drop-downs
        model.addAttribute("categories", Observation.Category.values());
        model.addAttribute("types", Observation.Type.values());
        model.addAttribute("statuses", Observation.Status.values());
        model.addAttribute("mitigations", Observation.Mitigation.values());
        model.addAttribute("areas", areaService.getAllAreas());

        return "observation/index";
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String observationDateFrom,
            @RequestParam(defaultValue = "") String observationDateTo,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String mitigation,
            @RequestParam(defaultValue = "") String areaCode,
            @RequestParam(defaultValue = "observationDate") String sortBy,
            @RequestParam(defaultValue = "false") String asc) {
        try {
            ObservationSearchCriteria criteria = ObservationSearchCriteria.of(
                    keyword, observationDateFrom, observationDateTo, category, type, status, mitigation, areaCode);
            return download(observationExportService.exportExcel(criteria, sortBy, "true".equalsIgnoreCase(asc)), "observation-report", "xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        } catch (Exception e) {
            log.error("Error exporting observations to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String observationDateFrom,
            @RequestParam(defaultValue = "") String observationDateTo,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String mitigation,
            @RequestParam(defaultValue = "") String areaCode,
            @RequestParam(defaultValue = "observationDate") String sortBy,
            @RequestParam(defaultValue = "false") String asc) {
        try {
            ObservationSearchCriteria criteria = ObservationSearchCriteria.of(
                    keyword, observationDateFrom, observationDateTo, category, type, status, mitigation, areaCode);
            return download(observationExportService.exportPdf(criteria, sortBy, "true".equalsIgnoreCase(asc)), "observation-report", "pdf", MediaType.APPLICATION_PDF);
        } catch (Exception e) {
            log.error("Error exporting observations to PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> download(byte[] content, String baseName, String extension, MediaType mediaType) {
        String filename = baseName + "-" + LocalDate.now() + "." + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(mediaType)
                .contentLength(content.length)
                .body(content);
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
