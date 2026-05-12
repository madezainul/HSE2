package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.dto.LoginRequest;
import ahqpck.hse.safety.model.dto.RegisterRequest;
import ahqpck.hse.safety.service.AuthenticationService;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.util.FileCategory;
import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

/**
 * Controller for handling authentication endpoints.
 * Manages login and registration pages and processes.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    /**
     * Display login page.
     * @param model the model
     * @return login template
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        log.debug("Displaying login page");
        model.addAttribute("pageTitle", "Login");
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    /**
     * Handle login request.
     * @param loginRequest the login request
     * @param bindingResult validation result
     * @param model the model
     * @param redirectAttributes redirect attributes
     * @return redirect to dashboard or login page with error
     */
    @PostMapping("/login")
    public String login(
        @Valid @ModelAttribute LoginRequest loginRequest,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {

        log.info("Login attempt for user: {}", loginRequest.getUsername());

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors in login request");
            model.addAttribute("pageTitle", "Login");
            return "auth/login";
        }

        try {
            authenticationService.authenticate(loginRequest);
            log.info("Login successful for user: {}", loginRequest.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Login successful!");
            return "redirect:/";

        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", loginRequest.getUsername());
            model.addAttribute("pageTitle", "Login");
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Invalid username or password");
            return "auth/login";

        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            model.addAttribute("pageTitle", "Login");
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            return "auth/login";
        }
    }

    /**
     * Display registration page.
     * @param model the model
     * @return register template
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.debug("Displaying registration page");
        model.addAttribute("pageTitle", "Register");
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    /**
     * Handle registration request.
     * @param registerRequest the registration request
     * @param bindingResult validation result
     * @param model the model
     * @param redirectAttributes redirect attributes
     * @return redirect to login page or register page with error
     */
    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute RegisterRequest registerRequest,
        BindingResult bindingResult,
        @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
        Model model,
        RedirectAttributes redirectAttributes) {

        log.info("Registration attempt for user: {}", registerRequest.getUsername());

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors in registration request");
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("registerRequest", registerRequest);
            return "auth/register";
        }

        try {
            boolean noLogin = Boolean.TRUE.equals(registerRequest.getNoLoginRequired());

            // Additional validation (skipped for no-login users)
            if (!noLogin) {
                if (registerRequest.getPassword() == null || registerRequest.getPassword().isBlank()) {
                    model.addAttribute("pageTitle", "Register");
                    model.addAttribute("registerRequest", registerRequest);
                    model.addAttribute("error", true);
                    model.addAttribute("errorMessage", "Password is required");
                    return "auth/register";
                }

                if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                    log.warn("Passwords do not match for registration");
                    model.addAttribute("pageTitle", "Register");
                    model.addAttribute("registerRequest", registerRequest);
                    model.addAttribute("error", true);
                    model.addAttribute("errorMessage", "Passwords do not match");
                    return "auth/register";
                }
            }

            if (!registerRequest.getAgreeToTerms()) {
                log.warn("User did not agree to terms and conditions");
                model.addAttribute("pageTitle", "Register");
                model.addAttribute("registerRequest", registerRequest);
                model.addAttribute("error", true);
                model.addAttribute("errorMessage", "You must agree to the terms and conditions");
                return "auth/register";
            }

            authenticationService.register(registerRequest);

            // Save profile picture if provided
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    String userId = userService.findByUsername(registerRequest.getUsername()).getId();
                    String picturePath = fileStorageService.storeFile(profilePicture, FileCategory.USER_PROFILE_IMAGE);
                    userService.updateProfilePicture(userId, picturePath);
                } catch (Exception picEx) {
                    log.warn("Failed to save profile picture during registration: {}", picEx.getMessage());
                }
            }

            log.info("Registration successful for user: {}", registerRequest.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", 
                "Registration successful! Please log in with your credentials.");
            return "redirect:/auth/login?success=true";

        } catch (ResourceAlreadyExistsException e) {
            log.warn("User already exists: {}", e.getMessage());
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";

        } catch (IllegalArgumentException e) {
            log.warn("Registration validation error: {}", e.getMessage());
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";

        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            return "auth/register";
        }
    }

    /**
     * Handle logout request.
     * @param redirectAttributes redirect attributes
     * @return redirect to login page
     */
    @PostMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        log.info("User logout request");
        authenticationService.logout();
        redirectAttributes.addFlashAttribute("logoutMessage", "You have been logged out successfully.");
        return "redirect:/auth/login?logout=true";
    }

    /**
     * Handle logout via GET (convenience endpoint).
     * @param redirectAttributes redirect attributes
     * @return redirect to login page
     */
    @GetMapping("/logout")
    public String logoutGet(RedirectAttributes redirectAttributes) {
        return logout(redirectAttributes);
    }
}
