package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.dto.CreateUserRequest;
import ahqpck.hse.safety.model.dto.ResetPasswordRequest;
import ahqpck.hse.safety.model.dto.UpdateOwnProfileRequest;
import ahqpck.hse.safety.model.dto.UpdateProfileRequest;
import ahqpck.hse.safety.model.dto.ChangePasswordRequest;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.exception.OperationNotAllowedException;
import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user-related operations.
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    /** Roles an administrator may give to a user. EMPLOYEE is for people who never sign in. */
    public static final List<String> ASSIGNABLE_ROLES = List.of("ADMIN", "MANAGER", "USER", "VIEWER", "EMPLOYEE");

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD_RULES =
        "Password must be at least 8 characters and contain uppercase, lowercase, and digits";

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

        // Email is optional: store "no email" as null so several users can leave it empty
        if (user.getEmail() != null && user.getEmail().isBlank()) {
            user.setEmail(null);
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            log.warn("Username already exists: {}", user.getUsername());
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmployeeId(user.getEmployeeId())) {
            log.warn("Employee ID already exists: {}", user.getEmployeeId());
            throw new ResourceAlreadyExistsException("Employee ID already exists");
        }

        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
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
     * Create a user on behalf of an administrator.
     * Validates the role and password, then delegates to {@link #createUser(User)}.
     * <p>
     * Email is optional. When invitation / password-reset emails are added, this is the place
     * to send the "your account is ready" message instead of the admin handing over the password.
     *
     * @param request the admin's create-user form
     * @return created user
     * @throws IllegalArgumentException if the role or password is invalid
     * @throws ResourceAlreadyExistsException if username, employee ID or email is already used
     */
    public User createUserByAdmin(CreateUserRequest request) {
        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        boolean noLogin = Boolean.TRUE.equals(request.getNoLoginRequired());
        String password;
        if (noLogin) {
            // Random unguessable password so this person cannot sign in
            password = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        } else {
            password = request.getPassword();
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (!password.equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            if (!isPasswordStrong(password)) {
                throw new IllegalArgumentException(PASSWORD_RULES);
            }
        }

        User user = User.builder()
            .username(request.getUsername().trim())
            .employeeId(request.getEmployeeId().trim())
            .email(request.getEmail() == null ? null : request.getEmail().trim())
            .fullName(request.getFullName().trim())
            .department(request.getDepartment().trim())
            .role(role)
            .password(password)
            .build();

        return createUser(user);
    }

    /**
     * Delete a user on behalf of an administrator.
     * <p>
     * Not allowed for your own account, for the last active administrator, or for a user that
     * other records point to (for example training records they created) - those users should
     * be deactivated instead.
     *
     * @param id the user to delete
     * @param actingUserId the id of the administrator doing the deletion
     * @throws ResourceNotFoundException if the user does not exist
     * @throws OperationNotAllowedException if a safeguard forbids the deletion
     */
    public void deleteUser(String id, String actingUserId) {
        log.info("Admin {} is deleting user id: {}", actingUserId, id);
        User user = findById(id);

        if (user.getId().equals(actingUserId)) {
            throw new OperationNotAllowedException("You cannot delete your own account.");
        }
        if (isActiveAdmin(user) && userRepository.countByRoleIgnoreCaseAndEnabledTrue(ADMIN_ROLE) <= 1) {
            throw new OperationNotAllowedException("You cannot delete the last active administrator.");
        }

        try {
            userRepository.delete(user);
            userRepository.flush(); // send the delete now so a blocked delete is reported here
        } catch (DataIntegrityViolationException e) {
            log.warn("User id {} could not be deleted because other records refer to it", id);
            throw new OperationNotAllowedException(
                "This user has records in the system and cannot be deleted. Deactivate the account instead.", e);
        }
        log.info("User deleted successfully with id: {}", id);
    }

    /**
     * Save an administrator's edits to a user: profile, role and active status together.
     * Either everything is saved or nothing is.
     * <p>
     * An administrator cannot deactivate or demote their own account, and the last active
     * administrator cannot be deactivated or demoted by anyone.
     *
     * @param id the user being edited
     * @param request the new profile values
     * @param enabled the new active status, or null to leave it unchanged
     * @param actingUserId the id of the administrator making the change
     * @return the updated user
     * @throws OperationNotAllowedException if a safeguard forbids the change
     */
    public User updateUserByAdmin(String id, UpdateProfileRequest request, Boolean enabled, String actingUserId) {
        User user = findById(id);

        // Work out what the account would be after this edit, and check the safeguards before changing anything
        boolean newEnabled = enabled != null ? enabled : Boolean.TRUE.equals(user.getEnabled());
        String newRole = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole() : user.getRole();
        boolean stillActiveAdmin = newEnabled && ADMIN_ROLE.equalsIgnoreCase(newRole == null ? "" : newRole.trim());

        if (isActiveAdmin(user) && !stillActiveAdmin) {
            if (user.getId().equals(actingUserId)) {
                throw new OperationNotAllowedException(
                    "You cannot deactivate your own account or remove your own administrator role.");
            }
            if (userRepository.countByRoleIgnoreCaseAndEnabledTrue(ADMIN_ROLE) <= 1) {
                throw new OperationNotAllowedException(
                    "At least one active administrator is required. Make another user an administrator first.");
            }
        }

        User updated = updateProfile(id, request);
        if (enabled != null) {
            log.info("Admin {} is setting enabled={} for user id: {}", actingUserId, enabled, id);
            updated.setEnabled(enabled);
            updated = userRepository.save(updated);
        }
        return updated;
    }

    /**
     * Set a new password for a user on behalf of an administrator (no current password needed).
     * <p>
     * Email is not set up yet, so the administrator gives the new password to the user directly.
     * When email is added, this is where a "reset your password" link would be sent instead.
     *
     * @param id the user whose password is reset
     * @param request the new password and its confirmation
     * @param actingUserId the id of the administrator doing the reset (for the log)
     * @throws ResourceNotFoundException if the user does not exist
     * @throws IllegalArgumentException if the passwords do not match or are too weak
     */
    public void resetPassword(String id, ResetPasswordRequest request, String actingUserId) {
        User user = findById(id);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (!isPasswordStrong(request.getNewPassword())) {
            throw new IllegalArgumentException(PASSWORD_RULES);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(true); // the user must pick their own password at the next sign-in
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        // Never log the password itself
        log.info("Admin {} reset the password of user id: {}", actingUserId, id);
    }

    private boolean isActiveAdmin(User user) {
        return Boolean.TRUE.equals(user.getEnabled()) && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
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

        // Email is optional: a blank email means "no email" and is stored as null
        String newEmail = (request.getEmail() == null || request.getEmail().isBlank())
            ? null
            : request.getEmail().trim();
        String newEmployeeId = request.getEmployeeId().trim();

        // Check if new email is already taken by another user
        if (newEmail != null && !newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            log.warn("Email already exists: {}", newEmail);
            throw new ResourceAlreadyExistsException("Email already exists: " + newEmail);
        }

        // Check if new employee ID is already taken by another user
        if (!newEmployeeId.equals(user.getEmployeeId()) && userRepository.existsByEmployeeId(newEmployeeId)) {
            log.warn("Employee ID already exists: {}", newEmployeeId);
            throw new ResourceAlreadyExistsException("Employee ID already exists: " + newEmployeeId);
        }

        user.setFullName(request.getFullName());
        user.setEmail(newEmail);
        user.setEmployeeId(newEmployeeId);
        user.setDepartment(request.getDepartment());
        // A blank role keeps the current one. Otherwise store it uppercase from the allowed list,
        // so old lowercase values ("admin") are corrected whenever an admin saves the user.
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String role = request.getRole().trim().toUpperCase();
            if (!ASSIGNABLE_ROLES.contains(role)) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
            user.setRole(role);
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

        // A temporary password handed out by an admin must not be kept
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        // Verify password strength
        if (!isPasswordStrong(request.getNewPassword())) {
            log.warn("Password does not meet strength requirements for user id: {}", id);
            throw new IllegalArgumentException(PASSWORD_RULES);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("Password changed successfully for user id: {}", id);
        return updated;
    }

    /**
     * Update the signed-in user's own name and email.
     * Employee ID, department and role are not touched - only an administrator changes those.
     *
     * @param id the user id (always the signed-in user's own id)
     * @param request the new name and email
     * @return updated user
     * @throws ResourceNotFoundException if user not found
     * @throws ResourceAlreadyExistsException if the email belongs to another user
     */
    public User updateOwnProfile(String id, UpdateOwnProfileRequest request) {
        User user = findById(id);

        // Email is optional: a blank email means "no email" and is stored as null
        String newEmail = (request.getEmail() == null || request.getEmail().isBlank())
            ? null
            : request.getEmail().trim();

        if (newEmail != null && !newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ResourceAlreadyExistsException("Email already exists: " + newEmail);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(newEmail);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("User id {} updated their own profile", id);
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
    /**
     * Search users for the management list. Blank filters are ignored.
     *
     * @param keyword text to look for in name, employee ID, username or email
     * @param role a role name (case does not matter); unknown values are ignored
     * @param active "active" or "inactive"; anything else means both
     * @param pageable the pagination parameters
     * @return page of matching users
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, String role, String active, Pageable pageable) {
        String likeKeyword = (keyword == null || keyword.isBlank())
            ? null
            : "%" + keyword.trim().toLowerCase() + "%";

        String normalizedRole = (role == null) ? "" : role.trim().toUpperCase();
        String roleFilter = ASSIGNABLE_ROLES.contains(normalizedRole) ? normalizedRole : null;

        Boolean enabledFilter = null;
        if ("active".equalsIgnoreCase(active)) {
            enabledFilter = true;
        } else if ("inactive".equalsIgnoreCase(active)) {
            enabledFilter = false;
        }

        return userRepository.search(likeKeyword, roleFilter, enabledFilter, pageable);
    }

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
