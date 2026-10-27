package ahqpck.hse.safety.config;

import ahqpck.hse.safety.model.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sends a signed-in user to the change-password page until they have chosen their own password.
 * <p>
 * Used after an administrator resets a password: the temporary password only gets the user
 * as far as the change-password page. The page itself, logout, and static files stay reachable.
 * Created by {@link SecurityConfig}; it is not a Spring bean on purpose, so it only runs inside
 * the security filter chain.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/profile/change-password";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user
                && Boolean.TRUE.equals(user.getMustChangePassword())
                && !isAllowedWhileChangeRequired(request)) {
            response.sendRedirect(request.getContextPath() + CHANGE_PASSWORD_PATH);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowedWhileChangeRequired(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(CHANGE_PASSWORD_PATH)
            || path.startsWith("/auth/")
            || path.startsWith("/assets/");
    }
}
