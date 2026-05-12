package ahqpck.hse.safety.controller.rest;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.service.AreaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
@Slf4j
public class AreaRestController {

    private final AreaService areaService;

    /**
     * Get areas for dropdown/live search functionality
     * @return list of active areas
     */
    @GetMapping("/dropdown")
    public ResponseEntity<?> getAreasForDropdown() {
        try {
            log.info("Fetching areas for dropdown");
            List<Area> areas = areaService.getActiveAreas();
            return ResponseEntity.ok(areas);
        } catch (Exception e) {
            log.error("Error fetching areas for dropdown", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch areas");
            return ResponseEntity.status(500).body(error);
        }
    }
}
