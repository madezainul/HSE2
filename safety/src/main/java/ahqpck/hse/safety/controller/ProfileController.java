package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import ahqpck.hse.safety.model.dto.ChangePasswordRequest;
import ahqpck.hse.safety.model.dto.UpdateOwnProfileRequest;
import ahqpck.hse.safety.model.dto.UserProfileResponse;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.mapper.UserMapper;
import ahqpck.hse.safety.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Pages every signed-in user can open for their own account: view profile,
 * edit name and email, and change password.
 * <p>
 * Everything here works on the signed-in user only; no user id is taken from the URL or the form,
 * so nobody can reach another person's profile through this controller.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("user", userMapper.toProfileResponse(user));
        return "profile/index";
    }

    @GetMapping("/edit")
    public String showEditProfile(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("updateOwnProfileRequest", UpdateOwnProfileRequest.builder()
            .fullName(user.getFullName())
            .email(user.getEmail())
            .build());
        model.addAttribute("user", userMapper.toProfileResponse(user));
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
        Authentication authentication,
        @Valid @ModelAttribute UpdateOwnProfileRequest request,
        BindingResult bindingResult,
        Model model) {

        User user = userService.findByUsername(authentication.getName());
        UserProfileResponse profile = userMapper.toProfileResponse(user);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", profile);
            return "profile/edit";
        }

        try {
            User updated = userService.updateOwnProfile(user.getId(), request);
            refreshSessionUser(authentication, updated);
            log.info("User {} updated their own profile", user.getUsername());
            return "redirect:/profile?updated=true";
        } catch (ResourceAlreadyExistsException e) {
            // "Email already exists: ..." - written for the user
            bindingResult.rejectValue("email", "error.email", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating profile for user {}", user.getUsername(), e);
            bindingResult.reject("profile.error", "Could not save your changes. Please try again.");
        }
        model.addAttribute("user", profile);
        return "profile/edit";
    }

    @GetMapping("/change-password")
    public String showChangePassword(Authentication authentication, Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        model.addAttribute("passwordChangeRequired", isChangeRequired(authentication));
        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
        Authentication authentication,
        @Valid @ModelAttribute ChangePasswordRequest request,
        BindingResult bindingResult,
        Model model) {

        model.addAttribute("passwordChangeRequired", isChangeRequired(authentication));

        if (bindingResult.hasErrors()) {
            return "profile/change-password";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            userService.changePassword(user.getId(), request);

            // The signed-in copy of the user still says "must change password"; clear it
            if (authentication.getPrincipal() instanceof User principal) {
                principal.setMustChangePassword(false);
            }
            log.info("User {} changed their password", user.getUsername());
            return "redirect:/profile?passwordChanged=true";
        } catch (IllegalArgumentException e) {
            // "Current password is incorrect", "New passwords do not match", password rules... written for the user
            bindingResult.reject("changePassword.failed", e.getMessage());
        } catch (Exception e) {
            log.error("Error changing password for user {}", authentication.getName(), e);
            bindingResult.reject("changePassword.error", "Could not change the password. Please try again.");
        }
        return "profile/change-password";
    }

    private boolean isChangeRequired(Authentication authentication) {
        return authentication.getPrincipal() instanceof User principal
            && Boolean.TRUE.equals(principal.getMustChangePassword());
    }

    /**
     * The signed-in user object is kept in the session, so copy the new name and email into it;
     * otherwise the page header would show the old values until the next sign-in.
     */
    private void refreshSessionUser(Authentication authentication, User updated) {
        if (authentication.getPrincipal() instanceof User principal) {
            principal.setFullName(updated.getFullName());
            principal.setEmail(updated.getEmail());
        }
    }
}
