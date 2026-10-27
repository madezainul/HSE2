package ahqpck.hse.safety.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("h2test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryQueryTest {

    @Autowired
    private UserRepository userRepository;

    private static final PageRequest ALL = PageRequest.of(0, 50, Sort.by("fullName"));

    private User make(String username, String empId, String fullName, String email, String role, boolean enabled) {
        return userRepository.saveAndFlush(User.builder()
                .username(username)
                .employeeId(empId)
                .fullName(fullName)
                .email(email)
                .password("x")
                .role(role)
                .enabled(enabled)
                .build());
    }

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        userRepository.flush();
        make("alice", "E001", "Alice Anderson", "alice@corp.com", "ADMIN", true);
        make("bob", "E002", "Bob Brown", null, "admin", true);           // NULL email, legacy lower-case role
        make("carol", "E003", "Carol Clark", "carol@other.org", "USER", true);
        make("dave", "E004", "Dave Davis", "dave@corp.com", "ADMIN", false);
        make("erin", "E005", "Erin Evans", "erin@corp.com", null, true); // NULL role
    }

    private List<String> names(Page<User> page) {
        return page.getContent().stream().map(User::getFullName).toList();
    }

    // ---- search ----

    @Test
    void search_allNullFilters_returnsEveryoneSortedByFullName() {
        Page<User> page = userRepository.search(null, null, null, ALL);
        assertThat(names(page)).containsExactly(
                "Alice Anderson", "Bob Brown", "Carol Clark", "Dave Davis", "Erin Evans");
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void search_keywordMatchesFullName() {
        assertThat(names(userRepository.search("%carol cl%", null, null, ALL))).containsExactly("Carol Clark");
    }

    @Test
    void search_keywordMatchesEmployeeId() {
        assertThat(names(userRepository.search("%e003%", null, null, ALL))).containsExactly("Carol Clark");
    }

    @Test
    void search_keywordMatchesUsername() {
        assertThat(names(userRepository.search("%dave%", null, null, ALL))).containsExactly("Dave Davis");
    }

    @Test
    void search_keywordMatchesEmail() {
        assertThat(names(userRepository.search("%@corp.com%", null, null, ALL)))
                .containsExactly("Alice Anderson", "Dave Davis", "Erin Evans");
    }

    @Test
    void search_userWithNullEmail_isStillFoundByOtherFields() {
        assertThat(names(userRepository.search("%bob%", null, null, ALL))).containsExactly("Bob Brown");
        assertThat(names(userRepository.search("%e002%", null, null, ALL))).containsExactly("Bob Brown");
    }

    @Test
    void search_userWithNullEmail_doesNotMatchEmailKeyword() {
        assertThat(names(userRepository.search("%@%", null, null, ALL)))
                .doesNotContain("Bob Brown")
                .hasSize(4);
    }

    @Test
    void search_keywordIsCaseInsensitiveAgainstStoredMixedCase() {
        // keyword is documented as lower-case; stored values are mixed case
        assertThat(names(userRepository.search("%alice anderson%", null, null, ALL))).containsExactly("Alice Anderson");
        assertThat(names(userRepository.search("%ALICE@CORP.COM%".toLowerCase(), null, null, ALL)))
                .containsExactly("Alice Anderson");
    }

    @Test
    void search_noMatch_returnsEmptyPage() {
        Page<User> page = userRepository.search("%zzz-nothing%", null, null, ALL);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void search_roleFilterMatchesBothAdminSpellings() {
        assertThat(names(userRepository.search(null, "ADMIN", null, ALL)))
                .containsExactly("Alice Anderson", "Bob Brown", "Dave Davis");
    }

    @Test
    void search_roleFilterExcludesUsersWithNullRole() {
        assertThat(names(userRepository.search(null, "USER", null, ALL))).containsExactly("Carol Clark");
    }

    @Test
    void search_enabledTrue_returnsOnlyActive() {
        assertThat(names(userRepository.search(null, null, true, ALL)))
                .containsExactly("Alice Anderson", "Bob Brown", "Carol Clark", "Erin Evans");
    }

    @Test
    void search_enabledFalse_returnsOnlyInactive() {
        assertThat(names(userRepository.search(null, null, false, ALL))).containsExactly("Dave Davis");
    }

    @Test
    void search_combinedFilters() {
        assertThat(names(userRepository.search("%corp.com%", "ADMIN", true, ALL))).containsExactly("Alice Anderson");
        assertThat(names(userRepository.search("%corp.com%", "ADMIN", false, ALL))).containsExactly("Dave Davis");
    }

    @Test
    void search_pagingReturnsRequestedSliceAndTotals() {
        Page<User> p0 = userRepository.search(null, null, null, PageRequest.of(0, 2, Sort.by("fullName")));
        Page<User> p1 = userRepository.search(null, null, null, PageRequest.of(1, 2, Sort.by("fullName")));
        Page<User> p2 = userRepository.search(null, null, null, PageRequest.of(2, 2, Sort.by("fullName")));
        assertThat(names(p0)).containsExactly("Alice Anderson", "Bob Brown");
        assertThat(names(p1)).containsExactly("Carol Clark", "Dave Davis");
        assertThat(names(p2)).containsExactly("Erin Evans");
        assertThat(p0.getTotalElements()).isEqualTo(5);
        assertThat(p0.getTotalPages()).isEqualTo(3);
    }

    @Test
    void search_sortDescendingByFullName() {
        Page<User> page = userRepository.search(null, null, null, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fullName")));
        assertThat(names(page)).containsExactly(
                "Erin Evans", "Dave Davis", "Carol Clark", "Bob Brown", "Alice Anderson");
    }

    @Test
    void search_pageBeyondEnd_isEmptyButTotalCorrect() {
        Page<User> page = userRepository.search(null, null, null, PageRequest.of(9, 10, Sort.by("fullName")));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    // ---- countByRoleIgnoreCaseAndEnabledTrue ----

    @Test
    void countActiveAdmins_countsMixedCaseAndSkipsDisabled() {
        // alice ADMIN, bob admin => 2 ; dave ADMIN is disabled
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).isEqualTo(2);
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("admin")).isEqualTo(2);
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("Admin")).isEqualTo(2);
    }

    @Test
    void countActiveAdmins_withExtraMixedCaseAdmin() {
        make("frank", "E006", "Frank Fox", "frank@corp.com", "Admin", true);
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).isEqualTo(3);
    }

    @Test
    void countActiveAdmins_unknownRoleIsZero() {
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("SUPERHERO")).isZero();
    }

    @Test
    void countActiveAdmins_allAdminsDisabled_isZero() {
        userRepository.findAll().stream()
                .filter(u -> "admin".equalsIgnoreCase(u.getRole()))
                .forEach(u -> { u.setEnabled(false); userRepository.save(u); });
        userRepository.flush();
        assertThat(userRepository.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).isZero();
    }

    // ---- finders / exists ----

    @Test
    void existsByUsername() {
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void existsByEmail() {
        assertThat(userRepository.existsByEmail("alice@corp.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@corp.com")).isFalse();
    }

    @Test
    void existsByEmployeeId() {
        assertThat(userRepository.existsByEmployeeId("E003")).isTrue();
        assertThat(userRepository.existsByEmployeeId("E999")).isFalse();
    }

    @Test
    void findByUsernameOrEmployeeId_matchesEither() {
        assertThat(userRepository.findByUsernameOrEmployeeId("alice")).get().extracting(User::getFullName).isEqualTo("Alice Anderson");
        assertThat(userRepository.findByUsernameOrEmployeeId("E002")).get().extracting(User::getFullName).isEqualTo("Bob Brown");
        assertThat(userRepository.findByUsernameOrEmployeeId("missing")).isEmpty();
    }

    @Test
    void findByEmail_nullEmailUsersAreNotReturnedForRealEmail() {
        assertThat(userRepository.findByEmail("carol@other.org")).get().extracting(User::getUsername).isEqualTo("carol");
        assertThat(userRepository.findByEmail("none@x.com")).isEmpty();
    }

    @Test
    void findByUsernameAndEmployeeId() {
        assertThat(userRepository.findByUsername("dave")).isPresent();
        assertThat(userRepository.findByEmployeeId("E005")).get().extracting(User::getUsername).isEqualTo("erin");
    }

    @Test
    void prePersist_generatesIdWhenMissing() {
        User u = make("gina", "E007", "Gina Green", "gina@corp.com", "USER", true);
        assertThat(u.getId()).isNotBlank();
        assertThat(userRepository.findById(u.getId())).isPresent();
    }
}
