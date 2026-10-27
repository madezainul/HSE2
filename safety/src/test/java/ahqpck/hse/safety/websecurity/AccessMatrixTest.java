package ahqpck.hse.safety.websecurity;

import ahqpck.hse.safety.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Who may open which page: /user/** is for administrators only, /profile/** for every signed-in user,
 * and everything else needs a login.
 */
class AccessMatrixTest extends WebSecurityTestBase {

    private static final String NEW_PASSWORD = "Brand9NewPass";

    /** Every administrator-only route of the user management pages. The delete is last on purpose. */
    private List<MockHttpServletRequestBuilder> adminOnlyRequests(String targetId) {
        return List.of(
            get("/user"),
            get("/user/new"),
            get("/user/{id}/edit", targetId),
            post("/user/{id}/reset-password", targetId)
                .param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD),
            post("/user/{id}/delete", targetId));
    }

    @Test
    void anonymousVisitorIsSentToLoginForEveryUserManagementRoute() throws Exception {
        User target = saveUser("USER");
        String hashBefore = target.getPassword();

        for (MockHttpServletRequestBuilder request : adminOnlyRequests(target.getId())) {
            mvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
        }

        User after = reload(target.getId());
        assertThat(after.getPassword()).as("password must not be reset by an anonymous request").isEqualTo(hashBefore);
        assertThat(after.getMustChangePassword()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "VIEWER", "EMPLOYEE", "SUPERVISOR"})
    void signedInNonAdminGetsForbiddenOnEveryUserManagementRoute(String role) throws Exception {
        User actor = saveUser(role);
        User target = saveUser("USER");
        String hashBefore = target.getPassword();

        for (MockHttpServletRequestBuilder request : adminOnlyRequests(target.getId())) {
            mvc.perform(request.with(as(actor))).andExpect(status().isForbidden());
        }

        // the forbidden delete and reset must really not have run
        User after = userRepository.findById(target.getId()).orElse(null);
        assertThat(after).as("target user must still exist").isNotNull();
        assertThat(after.getPassword()).isEqualTo(hashBefore);
        assertThat(after.getMustChangePassword()).isFalse();
    }

    @Test
    void userListWithTrailingSlashIsAlsoAdminOnly() throws Exception {
        User actor = saveUser("USER");
        mvc.perform(get("/user/").with(as(actor))).andExpect(status().isForbidden());
        mvc.perform(get("/user/list").with(as(actor))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenAllUserManagementPages() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(get("/user").with(as(admin)))
            .andExpect(status().isOk()).andExpect(view().name("user/index"));
        mvc.perform(get("/user/new").with(as(admin)))
            .andExpect(status().isOk()).andExpect(view().name("user/admin-create"));
        mvc.perform(get("/user/{id}/edit", target.getId()).with(as(admin)))
            .andExpect(status().isOk()).andExpect(view().name("user/admin-edit"));
        mvc.perform(get("/user/list").with(as(admin)))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/user"));
    }

    @Test
    void adminResetAndDeleteRoutesRedirectBackToTheList() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(post("/user/{id}/reset-password", target.getId())
                .param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD).with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/" + target.getId() + "/edit"));

        mvc.perform(post("/user/{id}/delete", target.getId()).with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"));
        assertThat(userRepository.findById(target.getId())).isEmpty();
    }

    @Test
    void oldUsersWithLowercaseAdminRoleStillGetAdminAccess() throws Exception {
        // CLAUDE/UserService notes: older users were stored as "admin"; getAuthorities() upper-cases the role
        User legacyAdmin = saveUser("admin");
        mvc.perform(get("/user").with(as(legacyAdmin))).andExpect(status().isOk());
    }

    @Test
    void userWithoutRoleIsTreatedAsPlainUser() throws Exception {
        User noRole = saveUser(null);
        mvc.perform(get("/user").with(as(noRole))).andExpect(status().isForbidden());
        mvc.perform(get("/profile").with(as(noRole))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/profile", "/profile/edit", "/profile/change-password"})
    void anonymousVisitorCannotOpenProfilePages(String path) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "VIEWER", "EMPLOYEE", "ADMIN"})
    void everySignedInRoleCanOpenOwnProfilePages(String role) throws Exception {
        User user = saveUser(role);
        mvc.perform(get("/profile").with(as(user)))
            .andExpect(status().isOk()).andExpect(view().name("profile/index"));
        mvc.perform(get("/profile/edit").with(as(user)))
            .andExpect(status().isOk()).andExpect(view().name("profile/edit"));
        mvc.perform(get("/profile/change-password").with(as(user)))
            .andExpect(status().isOk()).andExpect(view().name("profile/change-password"));
    }

    @Test
    void anonymousVisitorCannotOpenApplicationPages() throws Exception {
        for (String path : List.of("/incident", "/observation", "/training/toolbox", "/profile")) {
            mvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
        }
    }

    @Test
    void accessDeniedPageUsesTheFriendlyTemplate() throws Exception {
        User user = saveUser("USER");
        // The servlet container forwards a 403 to /error; this is the page the person sees
        mvc.perform(get("/error")
                .requestAttr("jakarta.servlet.error.status_code", 403)
                .accept(MediaType.TEXT_HTML)
                .with(as(user)))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("Access Denied")))
            .andExpect(content().string(containsString("You don't have permission to access this resource.")))
            .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void unknownPageGivesNotFoundForSignedInUser() throws Exception {
        User user = saveUser("USER");
        mvc.perform(get("/no-such-page-" + uniq()).with(as(user))).andExpect(status().isNotFound());
    }
}
