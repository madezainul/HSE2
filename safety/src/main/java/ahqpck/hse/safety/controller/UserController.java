package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.dto.ChangePasswordRequest;
import ahqpck.hse.safety.model.dto.UpdateProfileRequest;
import ahqpck.hse.safety.model.dto.UserProfileResponse;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.mapper.UserMapper;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.util.FileCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final UserService userService;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    /**
     * Display user management list with pagination.
     * Admin page to manage all users in the system.
     * @param page the page number (0-indexed)
     * @param size the page size
     * @param model the model
     * @return view name user/index.html
     */
    @GetMapping
    public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Model model) {
        log.info("Accessing user management list page with page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> usersPage = userService.getAllUsers(pageable);
            
            model.addAttribute("usersPage", usersPage);
            model.addAttribute("title", "Users Management");
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            log.info("User management page loaded successfully. Total users: {}", usersPage.getTotalElements());
            return "user/index";
        } catch (Exception e) {
            log.error("Error loading user list: {}", e.getMessage());
            model.addAttribute("error", "Error loading user list");
            return "redirect:/dashboard";
        }
    }

    /**
     * Display current user's profile page.
     * @param authentication the current user authentication
     * @param model the model
     * @return view name
     */
    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        log.info("Accessing user profile page for user: {}", authentication.getName());

        try {
            User user = userService.findByUsername(authentication.getName());
            UserProfileResponse profile = userMapper.toProfileResponse(user);
            model.addAttribute("user", profile);
            return "user/profile";
        } catch (Exception e) {
            log.error("Error loading user profile: {}", e.getMessage());
            model.addAttribute("error", "Error loading profile");
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
                .role(user.getRole())
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
     * @param enabled optional enabled flag
     * @param redirectAttributes redirect attributes
     * @param model the model
     * @return redirect to user list
     */
    @PostMapping("/{id}/edit")
    public String adminUpdateUser(
        @PathVariable String id,
        @Valid @ModelAttribute UpdateProfileRequest request,
        BindingResult bindingResult,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
        RedirectAttributes redirectAttributes,
        Model model) {

        log.info("Admin updating user id: {}", id);

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors updating user id {}: {}", id, bindingResult.getErrorCount());
            User user = userService.findById(id);
            model.addAttribute("targetUser", user);
            return "user/admin-edit";
        }

        try {
            userService.updateProfile(id, request);
            if (enabled != null) {
                userService.setUserEnabled(id, enabled);
            }
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String picturePath = fileStorageService.storeFile(profilePicture, FileCategory.USER_PROFILE_IMAGE);
                userService.updateProfilePicture(id, picturePath);
            }
            log.info("Admin updated user id: {} successfully", id);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
        } catch (Exception e) {
            log.error("Error updating user id {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/user";
    }

    /**
     * Delete a user by ID.
     * @param id the user ID
     * @param redirectAttributes redirect attributes
     * @return redirect to user list
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
        log.info("Admin deleting user id: {}", id);
        try {
            userService.deleteUser(id);
            log.info("User id: {} deleted successfully", id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting user id {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/user";
    }

    /**
     * Display edit profile page.
     * @param authentication the current user authentication
     * @param model the model
     * @return view name
     */
    @GetMapping("/profile/edit")
    public String showEditProfile(Authentication authentication, Model model) {
        log.info("Accessing edit profile page for user: {}", authentication.getName());

        try {
            User user = userService.findByUsername(authentication.getName());
            
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .role(user.getRole())
                .build();

            model.addAttribute("updateProfileRequest", request);
            model.addAttribute("userId", user.getId());
            return "user/edit";
        } catch (Exception e) {
            log.error("Error loading edit profile page: {}", e.getMessage());
            model.addAttribute("error", "Error loading profile");
            return "redirect:/user/profile";
        }
    }

    /**
     * Update user profile.
     * @param authentication the current user authentication
     * @param request the update profile request
     * @param bindingResult the binding result
     * @param redirectAttributes the redirect attributes
     * @return redirect to profile page
     */
    @PostMapping("/profile/edit")
    public String updateProfile(
        Authentication authentication,
        @Valid @ModelAttribute UpdateProfileRequest request,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {

        log.info("Updating profile for user: {}", authentication.getName());

        if (bindingResult.hasErrors()) {
            log.warn("Profile update validation errors: {}", bindingResult.getErrorCount());
            return "user/edit";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            userService.updateProfile(user.getId(), request);
            
            log.info("Profile updated successfully for user: {}", authentication.getName());
            redirectAttributes.addAttribute("updated", true);
            return "redirect:/user/profile";
        } catch (IllegalArgumentException e) {
            log.warn("Validation error during profile update: {}", e.getMessage());
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            return "user/edit";
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Display change password page.
     * @param model the model
     * @return view name
     */
    @GetMapping("/profile/change-password")
    public String showChangePassword(Model model) {
        log.info("Accessing change password page");
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "user/change-password";
    }

    /**
     * Change user password.
     * @param authentication the current user authentication
     * @param request the change password request
     * @param bindingResult the binding result
     * @param redirectAttributes the redirect attributes
     * @return redirect to profile page
     */
    @PostMapping("/profile/change-password")
    public String changePassword(
        Authentication authentication,
        @Valid @ModelAttribute ChangePasswordRequest request,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {

        log.info("Changing password for user: {}", authentication.getName());

        if (bindingResult.hasErrors()) {
            log.warn("Password change validation errors: {}", bindingResult.getErrorCount());
            return "user/change-password";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Additional validation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password confirmation mismatch for user: {}", authentication.getName());
                bindingResult.rejectValue("confirmPassword", "error.password", 
                    "Passwords do not match");
                return "user/change-password";
            }

            userService.changePassword(user.getId(), request);
            
            log.info("Password changed successfully for user: {}", authentication.getName());
            redirectAttributes.addAttribute("passwordChanged", true);
            return "redirect:/user/profile";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid password change request: {}", e.getMessage());
            bindingResult.rejectValue("currentPassword", "error.password", e.getMessage());
            return "user/change-password";
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error changing password");
            return "redirect:/user/profile";
        }
    }
}
