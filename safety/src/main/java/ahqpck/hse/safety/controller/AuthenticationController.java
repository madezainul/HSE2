package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.dto.LoginRequest;
import ahqpck.hse.safety.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

/**
 * Controller for handling authentication endpoints.
 * Manages the login page and process. There is no public sign-up: new users are
 * created by an administrator under /user/new.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

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
