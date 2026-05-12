package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.service.AreaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/areas")
@RequiredArgsConstructor
@Slf4j
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    public String listAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hiddenColumns,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            List<AreaDTO> areas = areaService.getAllAreas(keyword, sortBy, asc);

            model.addAttribute("areas", areas);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);
            model.addAttribute("title", "Areas");
            model.addAttribute("areaDTO", new AreaDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load areas: " + e.getMessage());
            return "error/500";
        }
        return "area/index";
    }

    @GetMapping("/{id}")
    public String viewArea(@PathVariable String id, Model model) {
        log.info("Viewing area with id: {}", id);
        Area area = areaService.getAreaById(id);
        
        if (area == null) {
            return "redirect:/areas";
        }
        
        model.addAttribute("area", area);
        model.addAttribute("pageTitle", "Area Details");
        
        return "area/view";
    }

    @PostMapping("/select-all")
    @ResponseBody
    public int selectAllAreas() {
        log.info("Select all areas request");
        return (int) areaService.getTotalAreaCount();
    }

    @GetMapping("/{id}/data")
    @ResponseBody
    public ResponseEntity<AreaDTO> getAreaData(@PathVariable String id) {
        Area area = areaService.getAreaById(id);
        if (area == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AreaDTO.fromEntity(area));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createArea(@RequestBody AreaDTO dto) {
        try {
            Area area = dto.toEntity();
            Area saved = areaService.createArea(area);
            return ResponseEntity.ok(Map.of("success", true, "id", saved.getId(),
                    "code", saved.getCode(), "name", saved.getName(),
                    "description", saved.getDescription() != null ? saved.getDescription() : "",
                    "status", saved.getStatus().toString()));
        } catch (Exception e) {
            log.error("Failed to create area: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateArea(@PathVariable String id, @RequestBody AreaDTO dto) {
        try {
            Area details = dto.toEntity();
            Area updated = areaService.updateArea(id, details);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("success", true, "id", updated.getId(),
                    "code", updated.getCode(), "name", updated.getName(),
                    "description", updated.getDescription() != null ? updated.getDescription() : "",
                    "status", updated.getStatus().toString()));
        } catch (Exception e) {
            log.error("Failed to update area {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/batch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAreas(@RequestBody List<String> ids) {
        log.info("Batch deleting {} areas", ids.size());
        int deleted = 0;
        for (String id : ids) {
            try {
                areaService.deleteArea(id);
                deleted++;
            } catch (Exception e) {
                log.warn("Failed to delete area id {}: {}", id, e.getMessage());
            }
        }
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

}
