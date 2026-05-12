package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.Accident;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.dto.AccidentDTO;
import ahqpck.hse.safety.model.dto.UserDTO;
import ahqpck.hse.safety.service.AccidentService;
import ahqpck.hse.safety.service.AreaService;
import ahqpck.hse.safety.service.UserService;
// import ahqpck.hse.safety.service.FileStorageServiceTrash;
import ahqpck.hse.safety.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/observation/accident")
@RequiredArgsConstructor
@Slf4j
public class AccidentController {

    private final AccidentService accidentService;
    private final AreaService areaService;
    private final UserService userService;
    // private final FileStorageServiceTrash fileStorageService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String listAccidents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accidentDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accidentDateTo,
            @RequestParam(required = false) String reportedBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String hiddenColumns,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "accidentDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Authentication authentication,
            Model model) {
        
        try {
            log.info("[listAccidents] page: {}, size: {}, status: {}, severity: {}, keyword: {}", 
                    page, size, status, severity, keyword);
            
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);
            Pageable pageable = PageRequest.of(zeroBasedPage, parsedSize);
            
            // Convert date range to LocalDateTime
            LocalDateTime from = accidentDateFrom != null ? accidentDateFrom.atStartOfDay() : null;
            LocalDateTime to = accidentDateTo != null ? accidentDateTo.atTime(LocalTime.MAX) : null;
            
            // Get current user
            UserDTO currentUser = getCurrentUser(authentication);
            
            // Get paginated accidents
            Page<Accident> accidentsPage = accidentService.getAccidentsPaginated(pageable);
            
            // Apply filters
            List<Accident> filteredAccidents = accidentsPage.getContent();
            
            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                try {
                    Accident.AccidentStatus statusEnum = Accident.AccidentStatus.valueOf(status);
                    filteredAccidents = filterAccidentsByStatus(filteredAccidents, statusEnum);
                } catch (IllegalArgumentException e) {
                    log.warn("[listAccidents] Invalid status filter: {}", status);
                }
            }
            
            // Filter by severity if provided
            if (severity != null && !severity.isEmpty()) {
                try {
                    Accident.SeverityLevel severityEnum = Accident.SeverityLevel.valueOf(severity);
                    filteredAccidents = filterAccidentsBySeverity(filteredAccidents, severityEnum);
                } catch (IllegalArgumentException e) {
                    log.warn("[listAccidents] Invalid severity filter: {}", severity);
                }
            }
            
            // Filter by accident date range
            if (from != null || to != null) {
                filteredAccidents = filterAccidentsByDateRange(filteredAccidents, from, to);
            }
            
            // Filter by keyword (code, description)
            if (keyword != null && !keyword.isEmpty()) {
                filteredAccidents = filterAccidentsByKeyword(filteredAccidents, keyword);
            }
            
            // Filter by reported by
            if (reportedBy != null && !reportedBy.isEmpty()) {
                filteredAccidents = filterAccidentsByReportedBy(filteredAccidents, reportedBy);
            }
            
            // Filter by area
            if (area != null && !area.isEmpty()) {
                filteredAccidents = filterAccidentsByArea(filteredAccidents, area);
            }
            
            // Add attributes to model
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("accidents", filteredAccidents);
            model.addAttribute("accidentsPage", accidentsPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("accidentDateFrom", accidentDateFrom);
            model.addAttribute("accidentDateTo", accidentDateTo);
            model.addAttribute("reportedBy", reportedBy);
            model.addAttribute("hiddenColumns", hiddenColumns);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);
            model.addAttribute("status", status);
            model.addAttribute("severity", severity);
            model.addAttribute("area", area);
            model.addAttribute("title", "Accident List");
            model.addAttribute("pageTitle", "Accidents Management");
            model.addAttribute("severityLevels", Accident.SeverityLevel.values());
            model.addAttribute("statuses", Accident.AccidentStatus.values());
            model.addAttribute("accidentDTO", new AccidentDTO());
            
        } catch (Exception e) {
            log.error("[listAccidents] Error loading accidents", e);
            model.addAttribute("error", "Failed to load accidents: " + e.getMessage());
            return "error/500";
        }
        
        return "accident/index";
    }
    
    // Helper filter methods
    private List<Accident> filterAccidentsByStatus(List<Accident> accidents, Accident.AccidentStatus status) {
        return accidents.stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }
    
    private List<Accident> filterAccidentsBySeverity(List<Accident> accidents, Accident.SeverityLevel severity) {
        return accidents.stream()
                .filter(a -> a.getSeverity() == severity)
                .toList();
    }
    
    private List<Accident> filterAccidentsByDateRange(List<Accident> accidents, LocalDateTime from, LocalDateTime to) {
        return accidents.stream()
                .filter(a -> {
                    LocalDateTime accidentDate = a.getAccidentDate();
                    if (accidentDate == null) return false;
                    if (from != null && accidentDate.isBefore(from)) return false;
                    if (to != null && accidentDate.isAfter(to)) return false;
                    return true;
                })
                .toList();
    }
    
    private List<Accident> filterAccidentsByKeyword(List<Accident> accidents, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return accidents.stream()
                .filter(a -> (a.getCode() != null && a.getCode().toLowerCase().contains(lowerKeyword)) ||
                             (a.getDescription() != null && a.getDescription().toLowerCase().contains(lowerKeyword)))
                .toList();
    }
    
    private List<Accident> filterAccidentsByReportedBy(List<Accident> accidents, String reportedBy) {
        return accidents.stream()
                .filter(a -> a.getReportedBy() != null && a.getReportedBy().equalsIgnoreCase(reportedBy))
                .toList();
    }
    
    private List<Accident> filterAccidentsByArea(List<Accident> accidents, String area) {
        return accidents.stream()
                .filter(a -> a.getArea() != null && 
                             (a.getArea().getCode().equalsIgnoreCase(area) || 
                              a.getArea().getName().toLowerCase().contains(area.toLowerCase())))
                .toList();
    }

    /**
     * View accident details
     * Path variable accidentId is actually the accident code (ACC-XXXXXX)
     * Service converts code to id for internal queries
     */
    @GetMapping("/{accidentId}")
    public String viewAccident(@PathVariable String accidentId, Model model) {
        log.info("[Controller] Viewing accident with code: {}", accidentId);
        
        // Look up by code (REST API uses code)
        Accident accident = accidentService.getAccidentByCode(accidentId);
        
        if (accident == null) {
            log.warn("Accident not found: {}", accidentId);
            return "redirect:/observation/accident";
        }
        
        model.addAttribute("accident", accident);
        model.addAttribute("title", "Accident List");
        model.addAttribute("pageTitle", "Accident Details");
        
        return "accident/detail";
    }

    /**
     * Edit accident details
     * Path variable accidentId is actually the accident code (ACC-XXXXXX)
     * Service converts code to id for internal queries
     */
    @GetMapping("/edit/{accidentId}")
    public String editAccident(@PathVariable String accidentId, Model model) {
        log.info("[Controller] Editing accident with code: {}", accidentId);
        
        // Look up by code (REST API uses code)
        Accident accident = accidentService.getAccidentByCode(accidentId);
        
        if (accident == null) {
            log.warn("Accident not found: {}", accidentId);
            return "redirect:/observation/accident";
        }
        
        model.addAttribute("accident", accident);
        model.addAttribute("title", "Accident List");
        model.addAttribute("pageTitle", "Edit Accident");
        model.addAttribute("severityLevels", Accident.SeverityLevel.values());
        model.addAttribute("statuses", Accident.AccidentStatus.values());
        
        return "accident/edit";
    }

    /**
     * Create accident via traditional form submission
     */
    @PostMapping
    public String createAccident(
            @Valid @ModelAttribute AccidentDTO accidentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "accidentImageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes ra) {

        log.info("[createAccident] Form submission received");
        if (imageFile != null && !imageFile.isEmpty()) {
            log.info("[createAccident] Image file received: name={}, size={} bytes, type={}", 
                    imageFile.getOriginalFilename(), imageFile.getSize(), imageFile.getContentType());
        } else {
            log.info("[createAccident] No image file in request");
        }

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            ra.addFlashAttribute("accidentDTO", accidentDTO);
            return "redirect:" + (redirectUrl != null ? redirectUrl : "/observation/accident");
        }

        try {
            // Get current user
            UserDTO currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                log.error("[createAccident] Could not extract user from authentication");
                ra.addFlashAttribute("error", "Authentication error: Could not extract user information. Please log in again.");
                ra.addFlashAttribute("accidentDTO", accidentDTO);
                return "redirect:" + (redirectUrl != null ? redirectUrl : "/observation/accident");
            }
            
            log.info("[createAccident] Creating accident for user: {}", currentUser.getId());
            
            // Store file if provided
            // String storedFileName = null;
            // if (imageFile != null && !imageFile.isEmpty()) {
            //     try {
            //         storedFileName = fileStorageService.storeFile(imageFile);
            //         log.info("[createAccident] File stored successfully: {}", storedFileName);
            //         accidentDTO.setAccidentImages(storedFileName);
            //     } catch (Exception e) {
            //         log.error("[createAccident] Error storing file", e);
            //         ra.addFlashAttribute("warning", "Accident created but file upload failed: " + e.getMessage());
            //     }
            // }
            
            // Create accident with stored file name
            log.info("[createAccident] About to create accident with DTO: reportedBy={}, images={}", 
                    accidentDTO.getReportedBy(), accidentDTO.getAccidentImages());
            Accident createdAccident = accidentService.createAccident(accidentDTO, null, currentUser.getId());
            log.info("[createAccident] Accident created successfully with code: {}", createdAccident.getCode());
            ra.addFlashAttribute("success", "Accident created successfully.");
        } catch (Exception e) {
            log.error("[createAccident] Error creating accident", e);
            ra.addFlashAttribute("error", "Error creating accident: " + e.getMessage());
            ra.addFlashAttribute("accidentDTO", accidentDTO);
        }
        return "redirect:" + (redirectUrl != null ? redirectUrl : "/observation/accident");
    }

    /**
     * REST API endpoint for file upload
     */
    // @PostMapping("/upload")
    // @ResponseBody
    // public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
    //     log.info("[uploadFile] File upload request received: name={}, size={} bytes, type={}", 
    //             file.getOriginalFilename(), file.getSize(), file.getContentType());
        
    //     try {
    //         String storedFileName = fileStorageService.storeFile(file);
    //         log.info("[uploadFile] File stored successfully: {}", storedFileName);
            
    //         Map<String, Object> response = new HashMap<>();
    //         response.put("fileName", storedFileName);
    //         response.put("originalFileName", file.getOriginalFilename());
    //         response.put("size", file.getSize());
    //         response.put("contentType", file.getContentType());
    //         response.put("message", "File uploaded successfully");
            
    //         return ResponseEntity.ok(response);
    //     } catch (Exception e) {
    //         log.error("[uploadFile] Error storing file", e);
            
    //         Map<String, Object> errorResponse = new HashMap<>();
    //         errorResponse.put("error", "File upload failed: " + e.getMessage());
            
    //         return ResponseEntity.badRequest().body(errorResponse);
    //     }
    // }

    /**
     * REST API endpoint for multiple file uploads
     */
    // @PostMapping("/upload-multiple")
    // @ResponseBody
    // public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
    //     log.info("[uploadMultipleFiles] Multiple file upload request received. File count: {}", files.length);
        
    //     List<Map<String, Object>> uploadResponses = new ArrayList<>();
        
    //     for (MultipartFile file : files) {
    //         Map<String, Object> response = new HashMap<>();
            
    //         try {
    //             String storedFileName = fileStorageService.storeFile(file);
    //             log.info("[uploadMultipleFiles] File stored successfully: {}", storedFileName);
                
    //             response.put("fileName", storedFileName);
    //             response.put("originalFileName", file.getOriginalFilename());
    //             response.put("size", file.getSize());
    //             response.put("contentType", file.getContentType());
    //             response.put("status", "success");
    //         } catch (Exception e) {
    //             log.error("[uploadMultipleFiles] Error storing file: {}", file.getOriginalFilename(), e);
    //             response.put("originalFileName", file.getOriginalFilename());
    //             response.put("error", e.getMessage());
    //             response.put("status", "failed");
    //         }
            
    //         uploadResponses.add(response);
    //     }
        
    //     return ResponseEntity.ok(uploadResponses);
    // }

    /**
     * Download file
     */
    // @GetMapping("/download/{fileName}")
    // public ResponseEntity<?> downloadFile(@PathVariable String fileName) {
    //     log.info("[downloadFile] Download request for file: {}", fileName);
        
    //     try {
    //         Resource resource = fileStorageService.loadFileAsResource(fileName);
            
    //         return ResponseEntity.ok()
    //                 .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
    //                 .body(resource);
    //     } catch (Exception e) {
    //         log.error("[downloadFile] Error downloading file", e);
            
    //         Map<String, String> errorResponse = new HashMap<>();
    //         errorResponse.put("error", "File download failed: " + e.getMessage());
            
    //         return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(errorResponse);
    //     }
    // }

    private UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("[getCurrentUser] Authentication is null");
            return null;
        }
        
        if (!authentication.isAuthenticated()) {
            log.warn("[getCurrentUser] User is not authenticated");
            return null;
        }
        
        log.debug("[getCurrentUser] Authentication principal type: {}", authentication.getPrincipal().getClass().getName());
        
        // Get the principal from authentication
        Object principal = authentication.getPrincipal();
        String username = null;
        
        // Try to extract username
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User springUser = 
                    (org.springframework.security.core.userdetails.User) principal;
            username = springUser.getUsername();
            log.debug("[getCurrentUser] Extracted username from spring user: {}", username);
        } else if (principal instanceof String) {
            username = (String) principal;
            log.debug("[getCurrentUser] Principal is string username: {}", username);
        } else {
            // Try to get username from authentication name
            username = authentication.getName();
            log.debug("[getCurrentUser] Got username from authentication.getName(): {}", username);
        }
        
        if (username == null || username.isEmpty()) {
            log.warn("[getCurrentUser] Could not extract username from authentication");
            return null;
        }
        
        // Load full User entity
        User user = userService.findByUsername(username);
        if (user != null) {
            log.info("[getCurrentUser] Found user in database: {}", user.getId());
            return UserDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .build();
        } else {
            log.warn("[getCurrentUser] User not found in database: {}", username);
            // Create a temporary UserDTO with username if user not found in DB
            // This allows the accident to be created with the authenticated username
            return UserDTO.builder()
                    .id(username) // Use username as ID if not found
                    .username(username)
                    .build();
        }
    }
}
