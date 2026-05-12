package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.LoginRequest;
import ahqpck.hse.safety.model.dto.RegisterRequest;
import ahqpck.hse.safety.model.dto.AuthResponse;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.util.Base62Utils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling authentication operations.
 * Manages login and registration processes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticate user with username/employee ID and password.
     * Supports login by either username or employee ID.
     * @param loginRequest the login request containing username (or employee ID) and password
     * @return AuthResponse with user details
     * @throws BadCredentialsException if authentication fails
     */
    public AuthResponse authenticate(LoginRequest loginRequest) {
        String credential = loginRequest.getUsername();
        log.info("Authenticating user with credential: {}", credential);

        try {
            // Try to authenticate with the credential (username or employee ID)
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    credential,
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Find user by username or employee ID
            User user = userService.findByUsernameOrEmployeeId(credential);

            log.info("User authenticated successfully: {}", credential);

            return AuthResponse.builder()
                .success(true)
                .message("Login successful")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .build();

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for credential: {}", credential);
            throw new BadCredentialsException("Invalid username/employee ID or password");
        }
    }

    /**
     * Register a new user.
     * @param registerRequest the registration request
     * @return AuthResponse with user details
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user with username: {}", registerRequest.getUsername());

        boolean noLogin = Boolean.TRUE.equals(registerRequest.getNoLoginRequired());

        if (!noLogin) {
            // Validate passwords match
            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                log.warn("Password confirmation does not match for username: {}", registerRequest.getUsername());
                throw new IllegalArgumentException("Passwords do not match");
            }

            // Validate password strength
            if (!isPasswordStrong(registerRequest.getPassword())) {
                log.warn("Password is not strong enough for username: {}", registerRequest.getUsername());
                throw new IllegalArgumentException("Password is not strong enough. " +
                    "It must contain at least one uppercase letter, one lowercase letter, and one digit.");
            }
        }

        // For no-login users generate a random unguessable password so they cannot log in
        String password = noLogin
            ? UUID.randomUUID().toString() + UUID.randomUUID().toString()
            : registerRequest.getPassword();

        // Create new user
        User newUser = User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .employeeId(registerRequest.getEmployeeId())
            .password(password)
            .fullName(registerRequest.getFullName())
            .department(registerRequest.getDepartment())
            .role(registerRequest.getRole())
            .build();

        User createdUser = userService.createUser(newUser);

        log.info("User registered successfully with username: {}", registerRequest.getUsername());

        return AuthResponse.builder()
            .success(true)
            .message("Registration successful. Please log in.")
            .userId(createdUser.getId())
            .username(createdUser.getUsername())
            .email(createdUser.getEmail())
            .employeeId(createdUser.getEmployeeId())
            .build();
    }

    /**
     * Get current authenticated user.
     * @return User entity
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching current user: {}", username);
        return userService.findByUsername(username);
    }

    /**
     * Logout current user.
     */
    public void logout() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User logged out: {}", username);
        SecurityContextHolder.clearContext();
    }

    /**
     * Validate password strength.
     * Password must contain:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * @param password the password to validate
     * @return true if password is strong
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUpperCase && hasLowerCase && hasDigit;
    }
}
