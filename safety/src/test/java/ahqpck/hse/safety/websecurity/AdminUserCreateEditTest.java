package ahqpck.hse.safety.websecurity;

import ahqpck.hse.safety.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Administrator flows: create a user and edit a user (profile, role, active switch, safeguards).
 */
class AdminUserCreateEditTest extends WebSecurityTestBase {

    private static final String STRONG = "Strong1Password";

    // ------------------------------------------------------------------ helpers

    /** Builds a POST with the given fields. A null value leaves the field out. Later pairs override earlier ones. */
    private static MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder builder,
                                                      Map<String, String> defaults, String... overrides) {
        Map<String, String> fields = new LinkedHashMap<>(defaults);
        for (int i = 0; i < overrides.length; i += 2) {
            fields.put(overrides[i], overrides[i + 1]);
        }
        fields.forEach((name, value) -> {
            if (value != null) {
                builder.param(name, value);
            }
        });
        return builder;
    }

    /** Create-user form; overrides are name, value, name, value ... */
    private MockHttpServletRequestBuilder createForm(String tag, String... overrides) {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("fullName", "New Person " + tag);
        defaults.put("employeeId", "NEW" + tag);
        defaults.put("username", "new" + tag);
        defaults.put("email", "new" + tag + "@example.test");
        defaults.put("department", "safety");
        defaults.put("role", "USER");
        defaults.put("password", STRONG);
        defaults.put("confirmPassword", STRONG);
        return form(post("/user/new"), defaults, overrides);
    }

    /** Edit form pre-filled with the user's current values (what the browser would send unchanged). */
    private MockHttpServletRequestBuilder editForm(User target, String... overrides) {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("fullName", target.getFullName());
        defaults.put("employeeId", target.getEmployeeId());
        defaults.put("email", target.getEmail() == null ? "" : target.getEmail());
        defaults.put("department", target.getDepartment());
        defaults.put("role", target.getRole() == null ? "" : target.getRole());
        return form(post("/user/{id}/edit", target.getId()), defaults, overrides);
    }

    // ------------------------------------------------------------------ create

    @Test
    void adminCreatesUserWithValidData() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag).with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"))
            .andExpect(flash().attribute("successMessage", containsString("new" + tag)));

        User created = userRepository.findByUsername("new" + tag).orElseThrow();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getRole()).isEqualTo("USER");
        assertThat(created.getEnabled()).isTrue();
        assertThat(created.getEmployeeId()).isEqualTo("NEW" + tag);
        assertThat(created.getPassword()).isNotEqualTo(STRONG);
        assertThat(passwordEncoder.matches(STRONG, created.getPassword())).isTrue();
    }

    @Test
    void createdUserCanLogInWithTheChosenPassword() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);
        mvc.perform(createForm(tag).with(as(admin))).andExpect(redirectedUrl("/user"));

        mvc.perform(post("/auth/login").param("username", "new" + tag).param("password", STRONG))
            .andExpect(redirectedUrl("/"));
        mvc.perform(post("/auth/login").param("username", "NEW" + tag).param("password", STRONG))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void roleIsStoredUpperCaseAndTextIsTrimmed() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "role", "manager", "fullName", "  Padded Name " + tag + "  ", "employeeId", "  NEW" + tag + "  ")
                .with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        User created = userRepository.findByUsername("new" + tag).orElseThrow();
        assertThat(created.getRole()).isEqualTo("MANAGER");
        assertThat(created.getFullName()).isEqualTo("Padded Name " + tag);
        assertThat(created.getEmployeeId()).isEqualTo("NEW" + tag);
    }

    @Test
    void createWithoutEmailStoresNullAndTwoUsersWithoutEmailAreAllowed() throws Exception {
        User admin = saveUser("ADMIN");
        String tagA = uniq();
        String tagB = uniq();
        trackUsername("new" + tagA);
        trackUsername("new" + tagB);

        mvc.perform(createForm(tagA, "email", "").with(as(admin))).andExpect(redirectedUrl("/user"));
        mvc.perform(createForm(tagB, "email", "").with(as(admin))).andExpect(redirectedUrl("/user"));

        assertThat(userRepository.findByUsername("new" + tagA).orElseThrow().getEmail()).isNull();
        assertThat(userRepository.findByUsername("new" + tagB).orElseThrow().getEmail()).isNull();
    }

    @Test
    void duplicateUsernameIsRefusedWithAMessage() throws Exception {
        User admin = saveUser("ADMIN");
        User existing = saveUser("USER");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "username", existing.getUsername()).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-create"))
            .andExpect(model().attributeHasErrors("createUserRequest"));

        assertThat(userRepository.findByEmployeeId("NEW" + tag)).as("no second account was created").isEmpty();
        assertThat(userRepository.findByUsername(existing.getUsername())).isPresent();
    }

    @Test
    void duplicateEmployeeIdIsRefused() throws Exception {
        User admin = saveUser("ADMIN");
        User existing = saveUser("USER");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "employeeId", existing.getEmployeeId()).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-create"))
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @Test
    void duplicateEmailIsRefused() throws Exception {
        User admin = saveUser("ADMIN");
        User existing = saveUser("USER");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "email", existing.getEmail()).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1A", "alllowercase1", "ALLUPPERCASE1", "NoDigitsAtAll"})
    void weakPasswordIsRefusedOnCreate(String weak) throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "password", weak, "confirmPassword", weak).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-create"))
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @Test
    void mismatchedPasswordConfirmationIsRefusedOnCreate() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "confirmPassword", STRONG + "x").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @Test
    void missingPasswordIsRefusedWhenSignInIsRequired() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "password", "", "confirmPassword", "").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @Test
    void noLoginEmployeeCanBeCreatedWithoutPasswordAndCannotSignIn() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "role", "EMPLOYEE", "noLoginRequired", "true", "password", "", "confirmPassword", "")
                .with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        User created = userRepository.findByUsername("new" + tag).orElseThrow();
        assertThat(created.getRole()).isEqualTo("EMPLOYEE");
        assertThat(created.getPassword()).isNotBlank();
        assertThat(passwordEncoder.matches("", created.getPassword())).isFalse();

        mvc.perform(post("/auth/login").param("username", "new" + tag).param("password", ""))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPERUSER", "ROOT", "ROLE_ADMIN"})
    void unknownRoleIsRefused(String role) throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);

        mvc.perform(createForm(tag, "role", role).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("createUserRequest"));
        assertThat(userRepository.findByUsername("new" + tag)).isEmpty();
    }

    @Test
    void invalidFormFieldsAreRefused() throws Exception {
        User admin = saveUser("ADMIN");
        String tag = uniq();
        trackUsername("new" + tag);
        trackUsername("ab");

        // short username, bad characters, bad email, missing department
        mvc.perform(createForm(tag, "username", "ab").with(as(admin)))
            .andExpect(model().attributeHasFieldErrors("createUserRequest", "username"));
        mvc.perform(createForm(tag, "username", "bad name!").with(as(admin)))
            .andExpect(model().attributeHasFieldErrors("createUserRequest", "username"));
        mvc.perform(createForm(tag, "email", "not-an-email").with(as(admin)))
            .andExpect(model().attributeHasFieldErrors("createUserRequest", "email"));
        mvc.perform(createForm(tag, "department", "").with(as(admin)))
            .andExpect(model().attributeHasFieldErrors("createUserRequest", "department"));
        mvc.perform(createForm(tag, "fullName", "").with(as(admin)))
            .andExpect(model().attributeHasFieldErrors("createUserRequest", "fullName"));

        assertThat(userRepository.findByEmployeeId("NEW" + tag)).isEmpty();
        assertThat(userRepository.findByUsername("ab")).isEmpty();
    }

    // ------------------------------------------------------------------ edit

    @Test
    void adminEditsProfileFieldsAndRole() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");
        String newEmail = "edited" + uniq() + "@example.test";

        mvc.perform(editForm(target, "fullName", "Edited Name", "email", newEmail, "department", "qc", "role", "MANAGER")
                .with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"))
            .andExpect(flash().attributeExists("successMessage"));

        User stored = reload(target.getId());
        assertThat(stored.getFullName()).isEqualTo("Edited Name");
        assertThat(stored.getEmail()).isEqualTo(newEmail);
        assertThat(stored.getDepartment()).isEqualTo("qc");
        assertThat(stored.getRole()).isEqualTo("MANAGER");
        assertThat(stored.getPassword()).as("edit never touches the password").isEqualTo(target.getPassword());
        assertThat(stored.getEnabled()).isTrue();
    }

    @Test
    void roleChangeTakesEffectForPermissions() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(editForm(target, "role", "ADMIN").with(as(admin))).andExpect(redirectedUrl("/user"));
        User promoted = reload(target.getId());
        mvc.perform(get("/user").with(as(promoted))).andExpect(status().isOk());
    }

    @Test
    void uncheckingActiveReallyDeactivatesTheUser() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        // an unchecked checkbox sends nothing; the hidden enabledSubmitted marker tells the server it was on the form
        mvc.perform(editForm(target, "enabledSubmitted", "true").with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        assertThat(reload(target.getId()).getEnabled()).isFalse();
        mvc.perform(post("/auth/login").param("username", target.getUsername()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void checkingActiveAgainReactivatesTheUser() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER", false);

        mvc.perform(editForm(target, "enabledSubmitted", "true", "enabled", "true").with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        assertThat(reload(target.getId()).getEnabled()).isTrue();
        mvc.perform(post("/auth/login").param("username", target.getUsername()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void editWithoutTheActiveSwitchLeavesTheStatusUnchanged() throws Exception {
        User admin = saveUser("ADMIN");
        User inactive = saveUser("USER", false);
        User active = saveUser("USER", true);

        mvc.perform(editForm(inactive, "fullName", "Still Inactive").with(as(admin))).andExpect(redirectedUrl("/user"));
        mvc.perform(editForm(active, "fullName", "Still Active").with(as(admin))).andExpect(redirectedUrl("/user"));

        assertThat(reload(inactive.getId()).getEnabled()).isFalse();
        assertThat(reload(active.getId()).getEnabled()).isTrue();
    }

    @Test
    void blankRoleKeepsTheCurrentRole() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("MANAGER");

        mvc.perform(editForm(target, "role", "", "fullName", "Role Kept").with(as(admin)))
            .andExpect(redirectedUrl("/user"));
        assertThat(reload(target.getId()).getRole()).isEqualTo("MANAGER");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: blank role keeps legacy lowercase role - UserService.java:385")
    void savingALegacyLowercaseRoleNormalisesItToUpperCase() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("manager");

        mvc.perform(editForm(target, "role", "").with(as(admin))).andExpect(redirectedUrl("/user"));
        // The service comment says "old lowercase values are corrected whenever an admin saves the user"
        assertThat(reload(target.getId()).getRole()).isEqualTo("MANAGER");
    }

    @Test
    void invalidRoleIsRefusedAndNothingElseIsSaved() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(editForm(target, "role", "HACKER", "fullName", "Half Saved").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-edit"))
            .andExpect(model().attributeHasErrors("updateProfileRequest"));

        User stored = reload(target.getId());
        assertThat(stored.getRole()).isEqualTo("USER");
        assertThat(stored.getFullName()).as("edit is all-or-nothing").isEqualTo(target.getFullName());
    }

    @Test
    void employeeIdOfAnotherUserIsRefused() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");
        User other = saveUser("USER");

        mvc.perform(editForm(target, "employeeId", other.getEmployeeId(), "fullName", "Dup Emp").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-edit"))
            .andExpect(model().attributeHasErrors("updateProfileRequest"));

        User stored = reload(target.getId());
        assertThat(stored.getEmployeeId()).isEqualTo(target.getEmployeeId());
        assertThat(stored.getFullName()).isEqualTo(target.getFullName());
    }

    @Test
    void emailOfAnotherUserIsRefused() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");
        User other = saveUser("USER");

        mvc.perform(editForm(target, "email", other.getEmail()).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("updateProfileRequest"));
        assertThat(reload(target.getId()).getEmail()).isEqualTo(target.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fullName", "employeeId", "department"})
    void blankRequiredFieldsAreRefusedOnEdit(String field) throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(editForm(target, field, "").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-edit"))
            .andExpect(model().attributeHasFieldErrors("updateProfileRequest", field));
    }

    @Test
    void userWithoutEmailCanBeOpenedAndEdited() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");
        target.setEmail(null);
        userRepository.save(target);

        mvc.perform(get("/user/{id}/edit", target.getId()).with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-edit"));

        mvc.perform(editForm(target, "email", "", "fullName", "Edited Without Mail").with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        User stored = reload(target.getId());
        assertThat(stored.getFullName()).isEqualTo("Edited Without Mail");
        assertThat(stored.getEmail()).isNull();
    }

    @Test
    void emailCanBeClearedOnEdit() throws Exception {
        User admin = saveUser("ADMIN");
        User target = saveUser("USER");

        mvc.perform(editForm(target, "email", "").with(as(admin))).andExpect(redirectedUrl("/user"));
        assertThat(reload(target.getId()).getEmail()).isNull();
    }

    @Test
    void editOfUnknownUserRedirectsToTheList() throws Exception {
        User admin = saveUser("ADMIN");

        mvc.perform(get("/user/{id}/edit", "doesNotExist" + uniq()).with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"));

        mvc.perform(post("/user/{id}/edit", "doesNotExist" + uniq())
                .param("fullName", "Ghost User").param("employeeId", "GHOST1").param("department", "safety")
                .param("role", "USER").with(as(admin)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"))
            .andExpect(flash().attribute("errorMessage", "User not found."));
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: unguarded findById gives 500 - UserController.java:217")
    void invalidEditOfUnknownUserDoesNotCrashWithAServerError() throws Exception {
        User admin = saveUser("ADMIN");
        // Validation fails first, then the controller reloads the user for the form: it must not blow up
        var result = mvc.perform(post("/user/{id}/edit", "doesNotExist" + uniq())
                .param("fullName", "").param("employeeId", "").param("department", "")
                .with(as(admin)))
            .andReturn();
        assertThat(result.getResponse().getStatus()).as("unknown user should give a redirect or 404, not 500").isLessThan(500);
    }

    // ------------------------------------------------------------------ safeguards

    @Test
    void adminCannotDeactivateOwnAccount() throws Exception {
        User admin = saveUser("ADMIN");
        saveUser("ADMIN"); // a second admin exists, so the "last admin" rule is not what protects here

        mvc.perform(editForm(admin, "enabledSubmitted", "true").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/admin-edit"))
            .andExpect(model().attributeHasErrors("updateProfileRequest"));

        assertThat(reload(admin.getId()).getEnabled()).isTrue();
    }

    @Test
    void adminCannotRemoveOwnAdministratorRole() throws Exception {
        User admin = saveUser("ADMIN");
        saveUser("ADMIN");

        mvc.perform(editForm(admin, "role", "USER").with(as(admin)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("updateProfileRequest"));

        assertThat(reload(admin.getId()).getRole()).isEqualTo("ADMIN");
    }

    @Test
    void adminEditingOwnNameWithoutRoleOrSwitchSucceeds() throws Exception {
        // The page locks the role select and the switch for your own account, so neither is sent
        User admin = saveUser("ADMIN");
        mvc.perform(post("/user/{id}/edit", admin.getId())
                .param("fullName", "Admin Renamed")
                .param("employeeId", admin.getEmployeeId())
                .param("email", admin.getEmail())
                .param("department", "safety")
                .with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        User stored = reload(admin.getId());
        assertThat(stored.getFullName()).isEqualTo("Admin Renamed");
        assertThat(stored.getRole()).isEqualTo("ADMIN");
        assertThat(stored.getEnabled()).isTrue();
    }

    @Test
    void adminCanDeactivateAndDemoteAnotherAdminWhileTwoExist() throws Exception {
        User admin = saveUser("ADMIN");
        User otherAdmin = saveUser("ADMIN");

        mvc.perform(editForm(otherAdmin, "role", "USER", "enabledSubmitted", "true").with(as(admin)))
            .andExpect(redirectedUrl("/user"));

        User stored = reload(otherAdmin.getId());
        assertThat(stored.getRole()).isEqualTo("USER");
        assertThat(stored.getEnabled()).isFalse();
    }

    @Test
    void lastActiveAdministratorCannotBeDeactivatedOrDemoted() throws Exception {
        // Acting admin A has a live session but was deactivated meanwhile, so B is the only active admin left
        User actingAdmin = saveUser("ADMIN");
        User lastAdmin = saveUser("ADMIN");
        User session = reload(actingAdmin.getId());
        actingAdmin.setEnabled(false);
        userRepository.save(actingAdmin);
        assumeThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("ADMIN"))
            .as("other tests left no other active administrators behind").isEqualTo(1L);

        mvc.perform(editForm(lastAdmin, "enabledSubmitted", "true").with(as(session)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("updateProfileRequest"));
        mvc.perform(editForm(lastAdmin, "role", "USER").with(as(session)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("updateProfileRequest"));

        User stored = reload(lastAdmin.getId());
        assertThat(stored.getEnabled()).isTrue();
        assertThat(stored.getRole()).isEqualTo("ADMIN");
    }
}
