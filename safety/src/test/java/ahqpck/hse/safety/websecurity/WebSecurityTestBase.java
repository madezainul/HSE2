package ahqpck.hse.safety.websecurity;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Shared set-up for the web security tests: real application context on the in-memory H2 database
 * ("h2test" profile, never MySQL), real security filter chain, real controllers and templates.
 * <p>
 * The tests are not wrapped in a transaction, so every web request commits like it does in production.
 * Everything a test creates is removed again in {@link #cleanUpCreatedData()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
abstract class WebSecurityTestBase {

    /** Password of every user created by {@link #saveUser}. Meets the password rules. */
    protected static final String RAW_PASSWORD = "Passw0rdOne";

    private static String cachedHash;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    private final Deque<Runnable> cleanups = new ArrayDeque<>();

    @AfterEach
    void cleanUpCreatedData() {
        while (!cleanups.isEmpty()) {
            try {
                cleanups.pop().run();
            } catch (RuntimeException ignored) {
                // best effort: a record that is already gone is fine
            }
        }
    }

    /** Registers work to run after the test, newest first. */
    protected void onCleanup(Runnable action) {
        cleanups.push(action);
    }

    /** Removes a user (created through the web form) by username after the test. */
    protected void trackUsername(String username) {
        onCleanup(() -> userRepository.findByUsername(username).ifPresent(userRepository::delete));
    }

    protected static String uniq() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private synchronized String encodedRawPassword() {
        if (cachedHash == null) {
            cachedHash = passwordEncoder.encode(RAW_PASSWORD);
        }
        return cachedHash;
    }

    protected User saveUser(String role) {
        return saveUser(role, true, false);
    }

    protected User saveUser(String role, boolean enabled) {
        return saveUser(role, enabled, false);
    }

    protected User saveUser(String role, boolean enabled, boolean mustChangePassword) {
        String tag = uniq();
        User user = User.builder()
            .username("usr" + tag)
            .employeeId("EMP" + tag)
            .email("usr" + tag + "@example.test")
            .password(encodedRawPassword())
            .fullName("Test User " + tag)
            .department("safety")
            .role(role)
            .enabled(enabled)
            .mustChangePassword(mustChangePassword)
            .build();
        return persist(user);
    }

    protected User persist(User user) {
        User saved = userRepository.save(user);
        String id = saved.getId();
        onCleanup(() -> userRepository.deleteById(id));
        return saved;
    }

    protected User reload(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    /** Signs the request in as this user, the way the form login does (the principal is the User entity). */
    protected RequestPostProcessor as(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
