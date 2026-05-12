package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.dto.UpdateProfileRequest;
import ahqpck.hse.safety.model.dto.ChangePasswordRequest;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user-related operations.
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Load user by username for Spring Security authentication.
     * Supports authentication by username or employee ID.
     * @param username the username or employee ID
     * @return UserDetails for the user
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for credential: {}", username);

        // Try to find user by username first, then by employee ID
        User user = userRepository.findByUsername(username)
            .or(() -> userRepository.findByEmployeeId(username))
            .orElseThrow(() -> {
                log.warn("User not found with username or employee ID: {}", username);
                return new UsernameNotFoundException("User not found with username or employee ID: " + username);
            });

        if (!user.getEnabled()) {
            log.warn("User account is disabled: {}", user.getUsername());
            throw new UsernameNotFoundException("User account is disabled");
        }

        return user;
    }

    /**
     * Find user by username.
     * @param username the username
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * Find user by email.
     * @param email the email
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Find user by ID.
     * @param id the user ID
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findById(String id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Find user by employee ID.
     * @param employeeId the employee ID
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByEmployeeId(String employeeId) {
        log.debug("Fetching user by employeeId: {}", employeeId);
        return userRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with employeeId: " + employeeId));
    }

    /**
     * Find user by username or employee ID.
     * Supports flexible login using either username or employee ID.
     * @param credential the username or employee ID
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByUsernameOrEmployeeId(String credential) {
        log.debug("Fetching user by username or employeeId: {}", credential);
        return userRepository.findByUsernameOrEmployeeId(credential)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username or employee ID: " + credential));
    }

    /**
     * Create a new user.
     * Generates a random Base62 UUID for the user if not already set.
     * @param user the user entity
     * @return created user
     * @throws ResourceAlreadyExistsException if username or email already exists
     */
    public User createUser(User user) {
        log.info("Creating new user with username: {}", user.getUsername());

        if (userRepository.existsByUsername(user.getUsername())) {
            log.warn("Username already exists: {}", user.getUsername());
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Email already exists: {}", user.getEmail());
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // ID (Base62 UUID) will be generated in @PrePersist
        User savedUser = userRepository.save(user);
        log.info("User created successfully with username: {} | Base62 UUID ID: {}", 
            savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }

    /**
     * Delete user by ID.
     * @param id the user ID
     */
    public void deleteUser(String id) {
        log.info("Deleting user with id: {}", id);
        User user = findById(id);
        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", id);
    }

    /**
     * Enable/disable user account.
     * @param id the user ID
     * @param enabled the enabled status
     */
    public void setUserEnabled(String id, Boolean enabled) {
        log.info("Setting user enabled status to {} for user id: {}", enabled, id);
        User user = findById(id);
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User enabled status updated successfully");
    }

    /**
     * Update user profile information.
     * @param id the user id
     * @param request the update profile request
     * @return updated user
     * @throws ResourceNotFoundException if user not found
     * @throws ResourceAlreadyExistsException if email already exists for another user
     * @throws ResourceAlreadyExistsException if employee ID already exists for another user
     */
    public User updateProfile(String id, UpdateProfileRequest request) {
        log.info("Updating profile for user id: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", id);
                return new ResourceNotFoundException("User not found with id: " + id);
            });

        // Check if new email is already taken by another user
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Email already exists: {}", request.getEmail());
                throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
            }
        }

        // Check if new employee ID is already taken by another user
        if (!user.getEmployeeId().equals(request.getEmployeeId())) {
            if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
                log.warn("Employee ID already exists: {}", request.getEmployeeId());
                throw new ResourceAlreadyExistsException("Employee ID already exists: " + request.getEmployeeId());
            }
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeId(request.getEmployeeId());
        user.setDepartment(request.getDepartment());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("Profile updated successfully for user id: {}", id);
        return updated;
    }

    /**
     * Change user password.
     * @param id the user id
     * @param request the change password request
     * @return updated user
     * @throws ResourceNotFoundException if user not found
     * @throws IllegalArgumentException if current password is incorrect or new password doesn't match confirmation
     */
    public User changePassword(String id, ChangePasswordRequest request) {
        log.info("Changing password for user id: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", id);
                return new ResourceNotFoundException("User not found with id: " + id);
            });

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Invalid current password for user id: {}", id);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Verify new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("New passwords do not match for user id: {}", id);
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Verify password strength
        if (!isPasswordStrong(request.getNewPassword())) {
            log.warn("Password does not meet strength requirements for user id: {}", id);
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and contain uppercase, lowercase, and digits"
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("Password changed successfully for user id: {}", id);
        return updated;
    }

    /**
     * Validate password strength.
     * @param password the password to validate
     * @return true if password meets strength requirements
     */
    /**
     * Update user profile picture path.
     * @param id the user ID
     * @param picturePath relative path returned by FileStorageService (e.g. "user-profile/images/uuid_file.jpg")
     */
    public void updateProfilePicture(String id, String picturePath) {
        User user = findById(id);
        user.setProfilePicture(picturePath);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Profile picture updated for user id: {}", id);
    }

    /**
     * Get all users with pagination.
     * @param pageable the pagination parameters
     * @return page of users
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");

        return hasUppercase && hasLowercase && hasDigit;
    }
}
