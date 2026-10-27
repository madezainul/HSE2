package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.exception.OperationNotAllowedException;
import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.dto.CreateUserRequest;
import ahqpck.hse.safety.model.dto.ResetPasswordRequest;
import ahqpck.hse.safety.model.dto.UpdateProfileRequest;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.util.FileCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final FileStorageService fileStorageService;

    /**
     * Display user management list with pagination.
     * Admin page to manage all users in the system.
     * @param page the page number (0-indexed)
     * @param size the page size (limited to 1-100)
     * @param keyword optional text to find in name, employee ID, username or email
     * @param role optional role filter
     * @param status optional "active" or "inactive" filter
     * @param model the model
     * @return view name user/index.html
     */
    @GetMapping
    public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String role,
        @RequestParam(defaultValue = "") String status,
        Model model) {
        // Keep page and size in a sane range so a URL like ?size=100000 cannot load every user at once
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        log.info("Accessing user management list page: page={}, size={}, keyword='{}', role='{}', status='{}'",
            safePage, safeSize, keyword, role, status);

        try {
            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("fullName").ignoreCase()));
            Page<User> usersPage = userService.searchUsers(keyword, role, status, pageable);

            model.addAttribute("usersPage", usersPage);
            model.addAttribute("title", "Users Management");
            model.addAttribute("currentPage", safePage);
            model.addAttribute("pageSize", safeSize);
            // Echoed back so the filter form keeps its values and the page links keep the filters
            model.addAttribute("keyword", keyword);
            model.addAttribute("role", role);
            model.addAttribute("status", status);

            log.info("User management page loaded successfully. Total users: {}", usersPage.getTotalElements());
            return "user/index";
        } catch (Exception e) {
            log.error("Error loading user list", e);
            return "redirect:/";
        }
    }

    /**
     * Redirect to user list page (user management).
     * @return redirect to user list
     */
    @GetMapping("/list")
    public String redirectToList() {
        return "redirect:/user";
    }

    /**
     * Display the form an administrator uses to add a new user.
     * @param model the model
     * @return view name user/admin-create.html
     */
    @GetMapping("/new")
    public String showCreateUser(Model model) {
        log.info("Admin accessing create user page");
        model.addAttribute("createUserRequest", new CreateUserRequest());
        model.addAttribute("title", "Users Management");
        return "user/admin-create";
    }

    /**
     * Create a new user. Admin only (enforced by SecurityConfig for /user/**).
     * @param request the create user form
     * @param bindingResult validation result
     * @param profilePicture optional profile picture
     * @param redirectAttributes redirect attributes
     * @param model the model
     * @return redirect to user list, or the form again with errors
     */
    @PostMapping("/new")
    public String createUser(
        @Valid @ModelAttribute CreateUserRequest request,
        BindingResult bindingResult,
        @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
        RedirectAttributes redirectAttributes,
        Model model) {

        log.info("Admin creating user with username: {}", request.getUsername());
        model.addAttribute("title", "Users Management");

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors creating user: {}", bindingResult.getErrorCount());
            return "user/admin-create";
        }

        try {
            User created = userService.createUserByAdmin(request);

            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    String picturePath = fileStorageService.storeFile(profilePicture, FileCategory.USER_PROFILE_IMAGE);
                    userService.updateProfilePicture(created.getId(), picturePath);
                } catch (Exception picEx) {
                    log.warn("User {} created but profile picture was not saved: {}", created.getUsername(), picEx.getMessage());
                    redirectAttributes.addFlashAttribute("errorMessage",
                        "User created, but the profile picture could not be saved.");
                    return "redirect:/user";
                }
            }

            log.info("Admin created user: {}", created.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                "User \"" + created.getUsername() + "\" created successfully.");
            return "redirect:/user";

        } catch (ResourceAlreadyExistsException | IllegalArgumentException e) {
            // Messages from these exceptions are written for the admin (e.g. "Username already exists")
            log.warn("Could not create user {}: {}", request.getUsername(), e.getMessage());
            bindingResult.reject("createUser.failed", e.getMessage());
            return "user/admin-create";
        } catch (Exception e) {
            log.error("Error creating user {}", request.getUsername(), e);
            bindingResult.reject("createUser.error", "Could not create the user. Please try again.");
            return "user/admin-create";
        }
    }

    /**
     * Display admin edit page for a specific user.
     * @param id the user ID
     * @param model the model
     * @return view name user/admin-edit.html
     */
    @GetMapping("/{id}/edit")
    public String showAdminEditUser(@PathVariable String id, Model model) {
        log.info("Admin accessing edit page for user id: {}", id);
        try {
            User user = userService.findById(id);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .department(user.getDepartment())
                // Older users have lowercase roles ("admin"); the role list uses uppercase
                .role(user.getRole() == null ? null : user.getRole().toUpperCase())
                .build();
            model.addAttribute("updateProfileRequest", request);
            model.addAttribute("targetUser", user);
            return "user/admin-edit";
        } catch (Exception e) {
            log.error("Error loading admin edit page for user id {}: {}", id, e.getMessage());
            return "redirect:/user";
        }
    }

    /**
     * Save admin edits for a specific user.
     * @param id the user ID
     * @param request the update profile request
     * @param bindingResult validation result
     * @param enabled the Active switch; an unchecked checkbox sends nothing, so it only counts when enabledSubmitted is true
     * @param enabledSubmitted true when the form contained the Active switch
     * @param redirectAttributes redirect attributes
     * @param model the model
     * @return redirect to user list, or the edit form again with errors
     */
    @PostMapping("/{id}/edit")
    public String adminUpdateUser(
        @PathVariable String id,
        @Valid @ModelAttribute UpdateProfileRequest request,
        BindingResult bindingResult,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(defaultValue = "false") boolean enabledSubmitted,
        @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        Model model) {

        log.info("Admin updating user id: {}", id);

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors updating user id {}: {}", id, bindingResult.getErrorCount());
            model.addAttribute("targetUser", userService.findById(id));
            return "user/admin-edit";
        }

        try {
            // Profile, role and active status are saved together, after the safeguards are checked
            Boolean newEnabled = enabledSubmitted ? Boolean.TRUE.equals(enabled) : null;
            userService.updateUserByAdmin(id, request, newEnabled, currentUserId(authentication));
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String picturePath = fileStorageService.storeFile(profilePicture, FileCategory.USER_PROFILE_IMAGE);
                userService.updateProfilePicture(id, picturePath);
            }
            log.info("Admin updated user id: {} successfully", id);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
            return "redirect:/user";

        } catch (ResourceAlreadyExistsException | IllegalArgumentException | OperationNotAllowedException e) {
            // e.g. "Employee ID already exists" - stay on the form so the admin does not lose the edits
            log.warn("Could not update user id {}: {}", id, e.getMessage());
            bindingResult.reject("updateUser.failed", e.getMessage());
            model.addAttribute("targetUser", userService.findById(id));
            return "user/admin-edit";
        } catch (ResourceNotFoundException e) {
            log.warn("User id {} not found while updating", id);
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/user";
        } catch (Exception e) {
            log.error("Error updating user id {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update user. Please try again.");
            return "redirect:/user";
        }
    }

    /**
     * Delete a user by ID.
     * @param id the user ID
     * @param redirectAttributes redirect attributes
     * @return redirect to user list
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable String id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        log.info("Admin deleting user id: {}", id);
        try {
            userService.deleteUser(id, currentUserId(authentication));
            log.info("User id: {} deleted successfully", id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (OperationNotAllowedException e) {
            // Written for the admin, e.g. "You cannot delete your own account."
            log.warn("Delete of user id {} refused: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.warn("User id {} not found while deleting", id);
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
        } catch (Exception e) {
            log.error("Error deleting user id {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete user. Please try again.");
        }
        return "redirect:/user";
    }

    /**
     * Set a new password for a user. Admin only (enforced by SecurityConfig for /user/**).
     * Email is not set up yet, so the admin passes the new password to the user directly.
     * @param id the user whose password is reset
     * @param request the new password and confirmation
     * @param bindingResult validation result
     * @param authentication the acting administrator
     * @param redirectAttributes redirect attributes
     * @return redirect back to the user's edit page
     */
    @PostMapping("/{id}/reset-password")
    public String resetPassword(
        @PathVariable String id,
        @Valid @ModelAttribute ResetPasswordRequest request,
        BindingResult bindingResult,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

        log.info("Admin resetting password for user id: {}", id);
        redirectAttributes.addAttribute("id", id); // fills {id} in the redirect below

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordError",
                bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/user/{id}/edit";
        }

        try {
            userService.resetPassword(id, request, currentUserId(authentication));
            redirectAttributes.addFlashAttribute("successMessage",
                "Password reset. Give the new password to the user; they will be asked to choose their own when they sign in.");
        } catch (IllegalArgumentException e) {
            // "Passwords do not match" / password rules - written for the admin
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.warn("User id {} not found while resetting password", id);
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/user";
        } catch (Exception e) {
            log.error("Error resetting password for user id {}", id, e);
            redirectAttributes.addFlashAttribute("passwordError", "Could not reset the password. Please try again.");
        }
        return "redirect:/user/{id}/edit";
    }

    /**
     * Id of the administrator making the current request.
     */
    private String currentUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).getId();
    }

}
