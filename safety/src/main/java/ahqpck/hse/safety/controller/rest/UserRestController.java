package ahqpck.hse.safety.controller.rest;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {

    private final UserRepository userRepository;

    /**
     * Get users for dropdown/live search functionality
     * @return list of active users with minimal data
     */
    @GetMapping("/dropdown")
    public ResponseEntity<?> getUsersForDropdown() {
        try {
            log.info("Fetching users for dropdown");
            List<User> users = userRepository.findAll();
            
            // Map to simple objects for frontend consumption
            List<Map<String, String>> response = users.stream()
                .map(user -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("employeeId", user.getEmployeeId());
                    map.put("fullName", user.getFullName());
                    map.put("username", user.getUsername());
                    return map;
                })
                .toList();
            
            log.info("Returning {} users for dropdown", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching users for dropdown", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch users");
            return ResponseEntity.status(500).body(error);
        }
    }
}
