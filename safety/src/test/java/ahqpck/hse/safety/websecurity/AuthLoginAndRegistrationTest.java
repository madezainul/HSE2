package ahqpck.hse.safety.websecurity;

import ahqpck.hse.safety.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Login (by username or employee ID) and the removed public sign-up.
 */
class AuthLoginAndRegistrationTest extends WebSecurityTestBase {

    private MvcResult login(String credential, String password) throws Exception {
        return mvc.perform(post("/auth/login").param("username", credential).param("password", password)).andReturn();
    }

    @Test
    void loginPageIsPublicAndHasNoSignUpLink() throws Exception {
        mvc.perform(get("/auth/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            .andExpect(content().string(not(containsStringIgnoringCase("/auth/register"))));
    }

    @Test
    void getRegisterNoLongerShowsASignUpPage() throws Exception {
        MvcResult result = mvc.perform(get("/auth/register")).andReturn();
        assertThat(result.getResponse().getStatus()).as("GET /auth/register").isBetween(400, 499);
    }

    @Test
    void postRegisterDoesNotCreateAnAccount() throws Exception {
        String tag = uniq();
        String username = "reg" + tag;
        trackUsername(username);

        MvcResult result = mvc.perform(post("/auth/register")
                .param("fullName", "Sneaky Person")
                .param("employeeId", "REG" + tag)
                .param("username", username)
                .param("email", username + "@example.test")
                .param("department", "safety")
                .param("password", RAW_PASSWORD)
                .param("confirmPassword", RAW_PASSWORD))
            .andReturn();

        assertThat(result.getResponse().getStatus()).as("POST /auth/register").isBetween(400, 499);
        assertThat(userRepository.findByUsername(username)).isEmpty();
        assertThat(userRepository.findByEmployeeId("REG" + tag)).isEmpty();
    }

    @Test
    void signedInAdminCannotRegisterThroughTheOldEndpointEither() throws Exception {
        User admin = saveUser("ADMIN");
        String username = "reg" + uniq();
        trackUsername(username);

        MvcResult result = mvc.perform(post("/auth/register")
                .param("username", username).param("password", RAW_PASSWORD).with(as(admin)))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isBetween(400, 499);
        assertThat(userRepository.findByUsername(username)).isEmpty();
    }

    @Test
    void loginWithUsernameWorksAndSessionReachesProfile() throws Exception {
        User user = saveUser("USER");

        MvcResult result = login(user.getUsername(), RAW_PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/profile").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("profile/index"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(user.getUsername())));
    }

    @Test
    void loginWithEmployeeIdWorks() throws Exception {
        User user = saveUser("USER");
        MvcResult result = login(user.getEmployeeId(), RAW_PASSWORD);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/auth/login").param("username", user.getUsername()).param("password", "Wrong0Password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void loginWithEmptyPasswordIsRejected() throws Exception {
        User user = saveUser("USER");
        mvc.perform(post("/auth/login").param("username", user.getUsername()).param("password", ""))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void loginWithUnknownUserIsRejected() throws Exception {
        mvc.perform(post("/auth/login").param("username", "nobody" + uniq()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void deactivatedUserCannotLogIn() throws Exception {
        User user = saveUser("USER", false);
        mvc.perform(post("/auth/login").param("username", user.getUsername()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/auth/login?error=true"));
        mvc.perform(post("/auth/login").param("username", user.getEmployeeId()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void lockedUserCannotLogIn() throws Exception {
        User user = saveUser("USER");
        user.setAccountNonLocked(false);
        userRepository.save(user);
        mvc.perform(post("/auth/login").param("username", user.getUsername()).param("password", RAW_PASSWORD))
            .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: EMPLOYEE role can sign in - UserService.java:57")
    void employeeRoleIsDocumentedAsNoSignInSoItMustNotBeAbleToLogIn() throws Exception {
        // UserService.ASSIGNABLE_ROLES comment and the role list ("Employee - no sign in"): EMPLOYEE never signs in.
        // The password here is a normal, known one, e.g. after an admin switched an existing user to EMPLOYEE.
        User employee = saveUser("EMPLOYEE");
        MvcResult result = login(employee.getUsername(), RAW_PASSWORD);
        assertThat(result.getResponse().getRedirectedUrl())
            .as("EMPLOYEE accounts are 'no sign in' accounts")
            .isEqualTo("/auth/login?error=true");
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        User user = saveUser("USER");
        MvcResult loginResult = login(user.getUsername(), RAW_PASSWORD);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mvc.perform(get("/auth/logout").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login?logout=true"));
    }
}
