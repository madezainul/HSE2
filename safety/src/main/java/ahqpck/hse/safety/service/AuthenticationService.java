package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.LoginRequest;
import ahqpck.hse.safety.model.dto.AuthResponse;
import ahqpck.hse.safety.model.entity.User;
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

}
