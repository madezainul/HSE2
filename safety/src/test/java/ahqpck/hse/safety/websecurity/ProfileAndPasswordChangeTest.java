package ahqpck.hse.safety.websecurity;

import ahqpck.hse.safety.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Own profile pages and the forced password change after an administrator reset.
 */
class ProfileAndPasswordChangeTest extends WebSecurityTestBase {

    private static final String CHANGE_PAGE = "/profile/change-password";
    private static final String NEW_PASSWORD = "Fresh7Password";

    // ------------------------------------------------------------------ forced password change

    @Test
    void userWhoMustChangePasswordIsRedirectedFromEveryPage() throws Exception {
        User user = saveUser("USER", true, true);
        for (String path : List.of("/profile", "/profile/edit", "/incident", "/observation",
                "/training/toolbox", "/", "/api/users/dropdown")) {
            mvc.perform(get(path).with(as(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(CHANGE_PAGE));
        }
    }

    @Test
    void adminWhoMustChangePasswordIsAlsoBlockedFromUserManagement() throws Exception {
        User admin = saveUser("ADMIN", true, true);
        mvc.perform(get("/user").with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(CHANGE_PAGE));
    }

    @Test
    void postsToOtherPagesAreBlockedWhilePasswordChangeIsRequired() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(post("/profile/edit").param("fullName", "Changed Name").param("email", "x@example.test").with(as(user)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(CHANGE_PAGE));
        assertThat(reload(user.getId()).getFullName()).isEqualTo(user.getFullName());
    }

    @Test
    void changePasswordPageStaysOpenAndSaysTheChangeIsRequired() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(get(CHANGE_PAGE).with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/change-password"))
            .andExpect(model().attribute("passwordChangeRequired", true));
    }

    @Test
    void logoutAndAssetsStayReachableWhilePasswordChangeIsRequired() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(get("/auth/logout").with(as(user)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login?logout=true"));

        MvcResult asset = mvc.perform(get("/assets/css/does-not-exist.css").with(as(user))).andReturn();
        assertThat(asset.getResponse().getRedirectedUrl()).isNotEqualTo(CHANGE_PAGE);
    }

    @Test
    void validPasswordChangeClearsTheFlagInTheDatabaseAndUnblocksTheUser() throws Exception {
        User user = saveUser("USER", true, true);

        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD)
                .with(as(user)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile?passwordChanged=true"));

        User stored = reload(user.getId());
        assertThat(stored.getMustChangePassword()).isFalse();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, stored.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored.getPassword())).isFalse();

        // the signed-in copy of the user was cleared too, so the next request is not redirected again
        mvc.perform(get("/profile").with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/index"));
    }

    @Test
    void fullLoginWithTemporaryPasswordThenChangeThenNormalUse() throws Exception {
        User user = saveUser("USER", true, true);

        MvcResult login = mvc.perform(post("/auth/login")
                .param("username", user.getUsername()).param("password", RAW_PASSWORD)).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/profile").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(CHANGE_PAGE));

        mvc.perform(post(CHANGE_PAGE).session(session)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD))
            .andExpect(redirectedUrl("/profile?passwordChanged=true"));

        mvc.perform(get("/profile").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/index"));
        assertThat(reload(user.getId()).getMustChangePassword()).isFalse();
    }

    @Test
    void wrongCurrentPasswordKeepsTheFlagAndPassword() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", "Wrong0Password")
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD)
                .with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/change-password"))
            .andExpect(model().attributeHasErrors("changePasswordRequest"));

        User stored = reload(user.getId());
        assertThat(stored.getMustChangePassword()).isTrue();
        assertThat(stored.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void mismatchedConfirmationIsRejected() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD + "x")
                .with(as(user)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("changePasswordRequest"));
        assertThat(reload(user.getId()).getMustChangePassword()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1A", "alllowercase1", "ALLUPPERCASE1", "NoDigitsAtAll"})
    void weakNewPasswordIsRejected(String weak) throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", weak)
                .param("confirmPassword", weak)
                .with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/change-password"))
            .andExpect(model().attributeHasErrors("changePasswordRequest"));

        User stored = reload(user.getId());
        assertThat(stored.getMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored.getPassword())).isTrue();
    }

    @Test
    void temporaryPasswordCannotBeKeptAsTheNewPassword() throws Exception {
        User user = saveUser("USER", true, true);
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", RAW_PASSWORD)
                .param("confirmPassword", RAW_PASSWORD)
                .with(as(user)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("changePasswordRequest"));
        assertThat(reload(user.getId()).getMustChangePassword()).isTrue();
    }

    @Test
    void emptyFieldsAreRejectedWithFieldErrors() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", "")
                .param("newPassword", "")
                .param("confirmPassword", "")
                .with(as(user)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("changePasswordRequest",
                "currentPassword", "newPassword", "confirmPassword"));
    }

    @Test
    void ordinaryUserCanChangePasswordWithoutBeingForced() throws Exception {
        User user = saveUser("USER");
        mvc.perform(get(CHANGE_PAGE).with(as(user)))
            .andExpect(status().isOk())
            .andExpect(model().attribute("passwordChangeRequired", false));

        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD)
                .with(as(user)))
            .andExpect(redirectedUrl("/profile?passwordChanged=true"));
        assertThat(passwordEncoder.matches(NEW_PASSWORD, reload(user.getId()).getPassword())).isTrue();
    }

    @Test
    void changePasswordOnlyAffectsTheSignedInUser() throws Exception {
        User me = saveUser("USER");
        User other = saveUser("USER");
        mvc.perform(post(CHANGE_PAGE)
                .param("currentPassword", RAW_PASSWORD)
                .param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD)
                .param("id", other.getId())
                .param("username", other.getUsername())
                .with(as(me)))
            .andExpect(status().is3xxRedirection());
        assertThat(reload(other.getId()).getPassword()).isEqualTo(other.getPassword());
    }

    // ------------------------------------------------------------------ own profile

    @Test
    void profilePageShowsOwnDataAndNeverThePasswordHash() throws Exception {
        User me = saveUser("USER");
        User other = saveUser("USER");

        MvcResult result = mvc.perform(get("/profile").param("username", other.getUsername()).with(as(me)))
            .andExpect(status().isOk())
            .andReturn();

        String html = body(result);
        assertThat(html).contains(me.getUsername());
        assertThat(html).doesNotContain(other.getUsername());
        assertThat(html).doesNotContain(me.getPassword());
        assertThat(html).doesNotContain("$2a$");
    }

    @Test
    void ownProfileUpdateSavesNameAndEmailAndRefreshesTheSessionUser() throws Exception {
        User user = saveUser("USER");
        String newEmail = "new" + uniq() + "@example.test";

        mvc.perform(post("/profile/edit")
                .param("fullName", "  Jane Updated  ")
                .param("email", newEmail)
                .with(as(user)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile?updated=true"));

        User stored = reload(user.getId());
        assertThat(stored.getFullName()).isEqualTo("Jane Updated");
        assertThat(stored.getEmail()).isEqualTo(newEmail);
        assertThat(user.getFullName()).as("signed-in copy is refreshed for the page header").isEqualTo("Jane Updated");
        assertThat(user.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void ownProfileUpdateCannotChangeRoleEmployeeIdOrPassword() throws Exception {
        User user = saveUser("USER");

        mvc.perform(post("/profile/edit")
                .param("fullName", "Still Me")
                .param("email", user.getEmail())
                .param("role", "ADMIN")
                .param("employeeId", "HACKED1")
                .param("department", "hacked")
                .param("enabled", "false")
                .param("password", "Hacked123Pass")
                .with(as(user)))
            .andExpect(redirectedUrl("/profile?updated=true"));

        User stored = reload(user.getId());
        assertThat(stored.getRole()).isEqualTo("USER");
        assertThat(stored.getEmployeeId()).isEqualTo(user.getEmployeeId());
        assertThat(stored.getDepartment()).isEqualTo("safety");
        assertThat(stored.getEnabled()).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored.getPassword())).isTrue();

        // and the role really did not leak into the permissions
        mvc.perform(get("/user").with(as(stored))).andExpect(status().isForbidden());
    }

    @Test
    void ownProfileKeepingTheSameEmailIsNotADuplicate() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/profile/edit").param("fullName", "Same Mail").param("email", user.getEmail()).with(as(user)))
            .andExpect(redirectedUrl("/profile?updated=true"));
    }

    @Test
    void ownProfileEmailUsedByAnotherUserIsRejected() throws Exception {
        User user = saveUser("USER");
        User other = saveUser("USER");

        mvc.perform(post("/profile/edit").param("fullName", "Taken Mail").param("email", other.getEmail()).with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/edit"))
            .andExpect(model().attributeHasFieldErrors("updateOwnProfileRequest", "email"));

        User stored = reload(user.getId());
        assertThat(stored.getEmail()).isEqualTo(user.getEmail());
        assertThat(stored.getFullName()).isEqualTo(user.getFullName());
    }

    @Test
    void blankEmailMeansNoEmailAndIsStoredAsNull() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/profile/edit").param("fullName", "No Mail").param("email", "").with(as(user)))
            .andExpect(redirectedUrl("/profile?updated=true"));
        assertThat(reload(user.getId()).getEmail()).isNull();
    }

    @Test
    void userWithoutEmailCanOpenAndSaveOwnProfile() throws Exception {
        User user = saveUser("USER");
        user.setEmail(null);
        userRepository.save(user);

        mvc.perform(get("/profile").with(as(user))).andExpect(status().isOk());
        mvc.perform(get("/profile/edit").with(as(user))).andExpect(status().isOk());
        mvc.perform(post("/profile/edit").param("fullName", "Renamed No Mail").param("email", "").with(as(user)))
            .andExpect(redirectedUrl("/profile?updated=true"));
        assertThat(reload(user.getId()).getFullName()).isEqualTo("Renamed No Mail");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "A"})
    void invalidFullNameIsRejected(String name) throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/profile/edit").param("fullName", name).param("email", user.getEmail()).with(as(user)))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/edit"))
            .andExpect(model().attributeHasFieldErrors("updateOwnProfileRequest", "fullName"));
        assertThat(reload(user.getId()).getFullName()).isEqualTo(user.getFullName());
    }

    @Test
    void invalidEmailFormatIsRejected() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/profile/edit").param("fullName", "Valid Name").param("email", "not-an-email").with(as(user)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("updateOwnProfileRequest", "email"));
    }

    @Test
    void profileEditPageDoesNotShowThePasswordHash() throws Exception {
        User user = saveUser("USER");
        mvc.perform(get("/profile/edit").with(as(user)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString(user.getPassword()))));
    }
}
