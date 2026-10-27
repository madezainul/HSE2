package ahqpck.hse.safety.users;

import ahqpck.hse.safety.config.PasswordChangeRequiredFilter;
import ahqpck.hse.safety.model.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordChangeRequiredFilterTest {

    private final PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Boolean mustChange) {
        User u = User.builder().id("1").username("u").mustChangePassword(mustChange).role("USER").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(u, "pw", u.getAuthorities()));
    }

    private MockHttpServletResponse run(String ctx, String uri, MockFilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setContextPath(ctx);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    private MockHttpServletResponse run(String ctx, String uri) throws Exception {
        return run(ctx, uri, new MockFilterChain());
    }

    private boolean passed(MockHttpServletResponse r) {
        return r.getRedirectedUrl() == null;
    }

    @Test
    void flaggedUser_redirectedFromPage() throws Exception {
        login(true);
        MockHttpServletResponse r = run("", "/incident");
        assertThat(r.getStatus()).isEqualTo(302);
        assertThat(r.getRedirectedUrl()).isEqualTo("/profile/change-password");
    }

    @Test
    void flaggedUser_redirectedFromApiAndRoot() throws Exception {
        login(true);
        assertThat(run("", "/").getRedirectedUrl()).isEqualTo("/profile/change-password");
        assertThat(run("", "/api/incidents").getRedirectedUrl()).isEqualTo("/profile/change-password");
    }

    @Test
    void flaggedUser_redirectIncludesContextPath() throws Exception {
        login(true);
        assertThat(run("/hse", "/hse/incident").getRedirectedUrl()).isEqualTo("/hse/profile/change-password");
    }

    @Test
    void flaggedUser_allowedPaths() throws Exception {
        login(true);
        assertThat(passed(run("", "/profile/change-password"))).isTrue();
        assertThat(passed(run("", "/auth/logout"))).isTrue();
        assertThat(passed(run("", "/auth/login"))).isTrue();
        assertThat(passed(run("", "/assets/css/app.css"))).isTrue();
    }

    @Test
    void flaggedUser_allowedPathsWithContextPath() throws Exception {
        login(true);
        assertThat(passed(run("/hse", "/hse/profile/change-password"))).isTrue();
        assertThat(passed(run("/hse", "/hse/assets/js/a.js"))).isTrue();
    }

    @Test
    void flaggedUser_lookAlikePathsStillRedirected() throws Exception {
        login(true);
        assertThat(run("", "/authority/x").getRedirectedUrl()).isNotNull();
        assertThat(run("", "/assetsX/a.js").getRedirectedUrl()).isNotNull();
        assertThat(run("", "/profile").getRedirectedUrl()).isNotNull();
    }

    @Test
    void unflaggedUser_passesThrough() throws Exception {
        login(false);
        assertThat(passed(run("", "/incident"))).isTrue();
    }

    @Test
    void nullFlag_passesThrough() throws Exception {
        login(null);
        assertThat(passed(run("", "/incident"))).isTrue();
    }

    @Test
    void anonymous_passesThrough() throws Exception {
        assertThat(passed(run("", "/incident"))).isTrue();
    }

    @Test
    void anonymousToken_passesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(passed(run("", "/incident"))).isTrue();
    }

    @Test
    void chainNotInvokedOnRedirect() throws Exception {
        login(true);
        MockFilterChain chain = new MockFilterChain();
        run("", "/incident", chain);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void chainInvokedWhenUnflagged() throws Exception {
        login(false);
        MockFilterChain chain = new MockFilterChain();
        run("", "/incident", chain);
        assertThat(chain.getRequest()).isNotNull();
    }
}
