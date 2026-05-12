package ahqpck.hse.safety.config;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.entity.Role;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Data initializer for development profile.
 * Creates default users and test data on application startup.
 */
@Component
@RequiredArgsConstructor
@Profile("dev")
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    @Value("${app.default-user.email:admin@safety.local}")
    private String defaultUserEmail;

    @Value("${app.default-user.employee_id:0904}")
    private String defaultUserEmployeeId;

    @Value("${app.default-user.fullname:Admin User}")
    private String defaultUserFullName;

    @Value("${app.default-user.username:admin}")
    private String defaultUserUsername;

    @Value("${app.default-user.password:Z@in1005}")
    private String defaultUserPassword;

    @Value("${app.default-user.department:Administration}")
    private String defaultUserDepartment;

    @Value("${app.default-user.role:ADMIN}")
    private String defaultUserRole;

    @Value("${app.default-user.enabled:true}")
    private Boolean defaultUserEnabled;

    /**
     * Initialize default data on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Starting data initialization...");
            initDefaultRoles();
            initDefaultUser();
            log.info("Data initialization completed successfully.");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    /**
     * Create default roles if they don't exist.
     */
    private void initDefaultRoles() {
        log.info("Initializing default roles...");
        Arrays.stream(Role.RoleName.values()).forEach(roleName -> {
            if (roleRepository.existsByName(roleName)) {
                log.debug("Role '{}' already exists.", roleName);
                return;
            }

            Role role = Role.builder()
                    .name(roleName)
                    .description(roleName.getDescription())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            roleRepository.save(role);
            log.info("Created role: {} - {}", roleName, roleName.getDescription());
        });
        log.info("Default roles initialization completed.");
    }

    /**
     * Create default admin user if it doesn't exist.
     */
    private void initDefaultUser() {
        if (!defaultUserEnabled) {
            log.debug("Default user creation is disabled");
            return;
        }

        log.info("Checking if default user with email '{}' exists...", defaultUserEmail);

        // Check if user already exists by email
        if (userRepository.findByEmail(defaultUserEmail).isPresent()) {
            log.info("Default user with email '{}' already exists. Skipping creation.", defaultUserEmail);
            return;
        }

        // Check if user already exists by employee ID
        if (userRepository.findByEmployeeId(defaultUserEmployeeId).isPresent()) {
            log.info("User with employee ID '{}' already exists. Skipping creation.", defaultUserEmployeeId);
            return;
        }

        // Check if user already exists by username
        if (userRepository.findByUsername(defaultUserUsername).isPresent()) {
            log.info("User with username '{}' already exists. Skipping creation.", defaultUserUsername);
            return;
        }

        log.info("Creating default user with email '{}'", defaultUserEmail);

        try {
            User user = User.builder()
                    .username(defaultUserUsername)
                    .employeeId(defaultUserEmployeeId)
                    .email(defaultUserEmail)
                    .password(defaultUserPassword) // Will be encoded by UserService.createUser()
                    .fullName(defaultUserFullName)
                    .department(defaultUserDepartment)
                    .role(defaultUserRole)
                    .enabled(defaultUserEnabled)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User createdUser = userService.createUser(user);
            log.info("Default user with email '{}' created successfully with Base62 UUID ID: {}", 
                defaultUserEmail, createdUser.getId());
        } catch (Exception e) {
            log.error("Failed to create default user with email '{}'", defaultUserEmail, e);
        }
    }
}
