package ahqpck.hse.safety.users;

import ahqpck.hse.safety.exception.OperationNotAllowedException;
import ahqpck.hse.safety.exception.ResourceAlreadyExistsException;
import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.dto.ChangePasswordRequest;
import ahqpck.hse.safety.model.dto.CreateUserRequest;
import ahqpck.hse.safety.model.dto.ResetPasswordRequest;
import ahqpck.hse.safety.model.dto.UpdateOwnProfileRequest;
import ahqpck.hse.safety.model.dto.UpdateProfileRequest;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    UserRepository repo;
    @Mock
    PasswordEncoder encoder;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repo, encoder);
        when(encoder.encode(anyString())).thenAnswer(i -> "ENC(" + i.getArgument(0) + ")");
        when(encoder.matches(anyString(), anyString()))
            .thenAnswer(i -> ("ENC(" + i.getArgument(0) + ")").equals(i.getArgument(1)));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private User user(String id, String role, boolean enabled) {
        return User.builder().id(id).username("user" + id).employeeId("E" + id)
            .email(id + "@x.com").fullName("Name " + id).department("Dept")
            .role(role).password("ENC(old)").enabled(enabled).build();
    }

    private void found(User u) {
        when(repo.findById(u.getId())).thenReturn(Optional.of(u));
    }

    private UpdateProfileRequest profile(User u, String role) {
        return UpdateProfileRequest.builder().fullName(u.getFullName()).email(u.getEmail())
            .employeeId(u.getEmployeeId()).department(u.getDepartment()).role(role).build();
    }

    private CreateUserRequest.CreateUserRequestBuilder adminReq() {
        return CreateUserRequest.builder().fullName(" Jane Doe ").employeeId(" E100 ")
            .username(" jane.doe ").email(" jane@x.com ").department(" HSE ").role("user")
            .password("Passw0rdX").confirmPassword("Passw0rdX");
    }

    // ---------------- createUser ----------------

    @Test
    void createUser_blankEmailStoredAsNull_andFlagsSet() {
        User u = User.builder().username("a1").employeeId("E1").email("   ").password("Plain1234").fullName("A").build();
        User saved = service.createUser(u);
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getPassword()).isEqualTo("ENC(Plain1234)");
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.isAccountNonExpired()).isTrue();
        assertThat(saved.isAccountNonLocked()).isTrue();
        assertThat(saved.isCredentialsNonExpired()).isTrue();
        verify(repo, never()).existsByEmail(any());
    }

    @Test
    void createUser_duplicateUsername() {
        when(repo.existsByUsername("a1")).thenReturn(true);
        User u = User.builder().username("a1").employeeId("E1").password("x").build();
        assertThatThrownBy(() -> service.createUser(u)).isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createUser_duplicateEmployeeId() {
        when(repo.existsByEmployeeId("E1")).thenReturn(true);
        User u = User.builder().username("a1").employeeId("E1").password("x").build();
        assertThatThrownBy(() -> service.createUser(u)).isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createUser_duplicateEmail() {
        when(repo.existsByEmail("a@x.com")).thenReturn(true);
        User u = User.builder().username("a1").employeeId("E1").email("a@x.com").password("x").build();
        assertThatThrownBy(() -> service.createUser(u)).isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createUser_forcesEnabledEvenIfCallerSetDisabled() {
        User u = User.builder().username("a1").employeeId("E1").password("x").enabled(false).build();
        assertThat(service.createUser(u).getEnabled()).isTrue();
    }

    // ---------------- createUserByAdmin ----------------

    @Test
    void createUserByAdmin_lowercaseRoleNormalised_andFieldsTrimmed() {
        User created = service.createUserByAdmin(adminReq().build());
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repo).save(cap.capture());
        User u = cap.getValue();
        assertThat(u.getRole()).isEqualTo("USER");
        assertThat(u.getUsername()).isEqualTo("jane.doe");
        assertThat(u.getEmployeeId()).isEqualTo("E100");
        assertThat(u.getEmail()).isEqualTo("jane@x.com");
        assertThat(u.getFullName()).isEqualTo("Jane Doe");
        assertThat(u.getDepartment()).isEqualTo("HSE");
        assertThat(created.getPassword()).isEqualTo("ENC(Passw0rdX)");
    }

    @Test
    void createUserByAdmin_roleWithSpacesAccepted() {
        service.createUserByAdmin(adminReq().role("  manager ").build());
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo("MANAGER");
    }

    @Test
    void createUserByAdmin_invalidRole() {
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().role("SUPERUSER").build()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid role");
        verify(repo, never()).save(any());
    }

    @Test
    void createUserByAdmin_nullOrBlankRole() {
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().role(null).build()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().role("  ").build()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUserByAdmin_noLogin_getsRandomPassword_noPasswordNeeded() {
        service.createUserByAdmin(adminReq().role("EMPLOYEE").noLoginRequired(true)
            .password(null).confirmPassword(null).build());
        service.createUserByAdmin(adminReq().username("other").role("EMPLOYEE").noLoginRequired(true)
            .password(null).confirmPassword(null).build());
        ArgumentCaptor<String> enc = ArgumentCaptor.forClass(String.class);
        verify(encoder, times(2)).encode(enc.capture());
        assertThat(enc.getAllValues().get(0)).hasSizeGreaterThanOrEqualTo(32);
        assertThat(enc.getAllValues().get(0)).isNotEqualTo(enc.getAllValues().get(1));
    }

    @Test
    void createUserByAdmin_noLogin_ignoresSuppliedWeakPassword() {
        service.createUserByAdmin(adminReq().role("EMPLOYEE").noLoginRequired(true)
            .password("weak").confirmPassword("different").build());
        ArgumentCaptor<String> enc = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(enc.capture());
        assertThat(enc.getValue()).isNotEqualTo("weak");
    }

    @Test
    void createUserByAdmin_missingPassword() {
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().password(null).build()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().password("   ").build()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUserByAdmin_mismatchedPassword() {
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().confirmPassword("Passw0rdY").build()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("do not match");
    }

    @Test
    void createUserByAdmin_weakPasswords() {
        for (String weak : List.of("Ab1", "alllowercase1", "ALLUPPER123", "NoDigitsHere")) {
            assertThatThrownBy(() -> service.createUserByAdmin(adminReq().password(weak).confirmPassword(weak).build()))
                .as(weak).isInstanceOf(IllegalArgumentException.class);
        }
        verify(repo, never()).save(any());
    }

    @Test
    void createUserByAdmin_blankEmailBecomesNull() {
        service.createUserByAdmin(adminReq().email("   ").build());
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isNull();
    }

    @Test
    void createUserByAdmin_duplicateUsernameAfterTrim() {
        when(repo.existsByUsername("jane.doe")).thenReturn(true);
        assertThatThrownBy(() -> service.createUserByAdmin(adminReq().build()))
            .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // ---------------- updateProfile ----------------

    @Test
    void updateProfile_userWithNullEmail_doesNotThrow() {
        User u = user("1", "USER", true);
        u.setEmail(null);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmail("new@x.com");
        assertThat(service.updateProfile("1", r).getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void updateProfile_nullEmailStaysNull() {
        User u = user("1", "USER", true);
        u.setEmail(null);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmail(null);
        assertThat(service.updateProfile("1", r).getEmail()).isNull();
    }

    @Test
    void updateProfile_blankEmailBecomesNull() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmail("  ");
        assertThat(service.updateProfile("1", r).getEmail()).isNull();
    }

    @Test
    void updateProfile_duplicateEmail() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmail("taken@x.com");
        when(repo.existsByEmail("taken@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile("1", r)).isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateProfile_sameOwnEmailIsNotDuplicate() {
        User u = user("1", "USER", true);
        found(u);
        when(repo.existsByEmail(u.getEmail())).thenReturn(true);
        assertThat(service.updateProfile("1", profile(u, null)).getEmail()).isEqualTo("1@x.com");
    }

    @Test
    void updateProfile_duplicateEmployeeId() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmployeeId("E999");
        when(repo.existsByEmployeeId("E999")).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile("1", r)).isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void updateProfile_employeeIdTrimmed() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmployeeId("  E777 ");
        assertThat(service.updateProfile("1", r).getEmployeeId()).isEqualTo("E777");
    }

    @Test
    void updateProfile_roleNormalisedUppercase() {
        User u = user("1", "admin", true);
        found(u);
        assertThat(service.updateProfile("1", profile(u, " manager ")).getRole()).isEqualTo("MANAGER");
    }

    @Test
    void updateProfile_invalidRole() {
        User u = user("1", "USER", true);
        found(u);
        assertThatThrownBy(() -> service.updateProfile("1", profile(u, "GOD")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateProfile_blankRoleKeepsCurrent() {
        User u = user("1", "VIEWER", true);
        found(u);
        assertThat(service.updateProfile("1", profile(u, "  ")).getRole()).isEqualTo("VIEWER");
        assertThat(service.updateProfile("1", profile(u, null)).getRole()).isEqualTo("VIEWER");
    }

    @Test
    void updateProfile_notFound() {
        when(repo.findById("zz")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProfile("zz", new UpdateProfileRequest("n", null, "E", "d", null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_invalidRoleNeverSaves() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, "GOD");
        assertThatThrownBy(() -> service.updateProfile("1", r)).isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    // ---------------- updateUserByAdmin ----------------

    @Test
    void updateUserByAdmin_adminCannotDeactivateSelf() {
        User admin = user("1", "ADMIN", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(3L);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(admin, null), false, "1"))
            .isInstanceOf(OperationNotAllowedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateUserByAdmin_adminCannotDemoteSelf() {
        User admin = user("1", "ADMIN", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(3L);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(admin, "USER"), null, "1"))
            .isInstanceOf(OperationNotAllowedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateUserByAdmin_lowercaseAdminSelfCannotDemote() {
        User admin = user("1", "admin", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(3L);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(admin, "viewer"), null, "1"))
            .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    void updateUserByAdmin_lastActiveAdminCannotBeDeactivatedByAnother() {
        User admin = user("1", "ADMIN", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(1L);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(admin, null), false, "2"))
            .isInstanceOf(OperationNotAllowedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateUserByAdmin_lastActiveAdminCannotBeDemotedByAnother() {
        User admin = user("1", "admin", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(1L);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(admin, "USER"), null, "2"))
            .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    void updateUserByAdmin_otherAdminMayDemoteWhenAnotherAdminRemains() {
        User admin = user("1", "ADMIN", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(2L);
        User out = service.updateUserByAdmin("1", profile(admin, "USER"), false, "2");
        assertThat(out.getRole()).isEqualTo("USER");
        assertThat(out.getEnabled()).isFalse();
    }

    @Test
    void updateUserByAdmin_enabledNullLeavesStatus() {
        User u = user("1", "USER", false);
        found(u);
        assertThat(service.updateUserByAdmin("1", profile(u, null), null, "2").getEnabled()).isFalse();
    }

    @Test
    void updateUserByAdmin_canEnableUser() {
        User u = user("1", "USER", false);
        found(u);
        assertThat(service.updateUserByAdmin("1", profile(u, null), true, "2").getEnabled()).isTrue();
    }

    @Test
    void updateUserByAdmin_selfKeepingAdminAndActiveIsAllowed() {
        User admin = user("1", "admin", true);
        found(admin);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(1L);
        assertThat(service.updateUserByAdmin("1", profile(admin, "ADMIN"), true, "1").getRole()).isEqualTo("ADMIN");
    }

    @Test
    void updateUserByAdmin_invalidRoleAppliesNothing() {
        User u = user("1", "USER", true);
        found(u);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", profile(u, "GOD"), false, "2"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
        assertThat(u.getEnabled()).isTrue();
    }

    @Test
    void updateUserByAdmin_duplicateEmailAppliesNothing() {
        User u = user("1", "USER", true);
        found(u);
        UpdateProfileRequest r = profile(u, null);
        r.setEmail("taken@x.com");
        when(repo.existsByEmail("taken@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.updateUserByAdmin("1", r, false, "2"))
            .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
        assertThat(u.getEnabled()).isTrue();
    }

    @Test
    void updateUserByAdmin_notFound() {
        when(repo.findById("9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUserByAdmin("9", new UpdateProfileRequest(), true, "1"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUserByAdmin_deactivatingNonAdminNeverCountsAdmins() {
        User u = user("1", "USER", true);
        found(u);
        service.updateUserByAdmin("1", profile(u, null), false, "2");
        verify(repo, never()).countByRoleIgnoreCaseAndEnabledTrue(any());
    }

    // ---------------- deleteUser ----------------

    @Test
    void deleteUser_self() {
        found(user("1", "USER", true));
        assertThatThrownBy(() -> service.deleteUser("1", "1")).isInstanceOf(OperationNotAllowedException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteUser_lastActiveAdmin() {
        found(user("1", "admin", true));
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteUser("1", "2")).isInstanceOf(OperationNotAllowedException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteUser_adminWhenOthersRemain() {
        User u = user("1", "ADMIN", true);
        found(u);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(2L);
        service.deleteUser("1", "2");
        verify(repo).delete(u);
        verify(repo).flush();
    }

    @Test
    void deleteUser_inactiveAdminCanBeDeleted() {
        User u = user("1", "ADMIN", false);
        found(u);
        when(repo.countByRoleIgnoreCaseAndEnabledTrue("ADMIN")).thenReturn(1L);
        service.deleteUser("1", "2");
        verify(repo).delete(u);
    }

    @Test
    void deleteUser_integrityViolationMapped() {
        found(user("1", "USER", true));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk")).when(repo).flush();
        assertThatThrownBy(() -> service.deleteUser("1", "2"))
            .isInstanceOf(OperationNotAllowedException.class)
            .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteUser_notFound() {
        when(repo.findById("9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteUser("9", "2")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- resetPassword ----------------

    @Test
    void resetPassword_success_setsMustChange_andEncodes() {
        User u = user("1", "USER", true);
        found(u);
        service.resetPassword("1", new ResetPasswordRequest("NewPass123", "NewPass123"), "2");
        assertThat(u.getMustChangePassword()).isTrue();
        assertThat(u.getPassword()).isEqualTo("ENC(NewPass123)").isNotEqualTo("NewPass123");
        assertThat(u.getUpdatedAt()).isNotNull();
        verify(repo).save(u);
    }

    @Test
    void resetPassword_mismatch() {
        User u = user("1", "USER", true);
        found(u);
        assertThatThrownBy(() -> service.resetPassword("1", new ResetPasswordRequest("NewPass123", "NewPass124"), "2"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
        assertThat(u.getMustChangePassword()).isFalse();
    }

    @Test
    void resetPassword_weak() {
        found(user("1", "USER", true));
        assertThatThrownBy(() -> service.resetPassword("1", new ResetPasswordRequest("weak", "weak"), "2"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void resetPassword_notFound() {
        when(repo.findById("9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("9", new ResetPasswordRequest("NewPass123", "NewPass123"), "2"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- changePassword ----------------

    private User withPassword(String plain) {
        User u = user("1", "USER", true);
        u.setPassword("ENC(" + plain + ")");
        u.setMustChangePassword(true);
        found(u);
        return u;
    }

    @Test
    void changePassword_success_clearsMustChange() {
        User u = withPassword("OldPass123");
        service.changePassword("1", new ChangePasswordRequest("OldPass123", "NewPass123", "NewPass123"));
        assertThat(u.getMustChangePassword()).isFalse();
        assertThat(u.getPassword()).isEqualTo("ENC(NewPass123)");
    }

    @Test
    void changePassword_wrongCurrent() {
        withPassword("OldPass123");
        assertThatThrownBy(() -> service.changePassword("1", new ChangePasswordRequest("Nope12345", "NewPass123", "NewPass123")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Current password");
        verify(repo, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrent() {
        User u = withPassword("OldPass123");
        assertThatThrownBy(() -> service.changePassword("1", new ChangePasswordRequest("OldPass123", "OldPass123", "OldPass123")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(u.getMustChangePassword()).isTrue();
    }

    @Test
    void changePassword_mismatch() {
        withPassword("OldPass123");
        assertThatThrownBy(() -> service.changePassword("1", new ChangePasswordRequest("OldPass123", "NewPass123", "NewPass999")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void changePassword_weak() {
        withPassword("OldPass123");
        assertThatThrownBy(() -> service.changePassword("1", new ChangePasswordRequest("OldPass123", "short", "short")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void changePassword_notFound() {
        when(repo.findById("9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changePassword("9", new ChangePasswordRequest("a", "b", "b")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- updateOwnProfile ----------------

    @Test
    void updateOwnProfile_changesOnlyNameAndEmail() {
        found(user("1", "ADMIN", true));
        User out = service.updateOwnProfile("1", new UpdateOwnProfileRequest("  New Name ", " new@x.com "));
        assertThat(out.getFullName()).isEqualTo("New Name");
        assertThat(out.getEmail()).isEqualTo("new@x.com");
        assertThat(out.getEmployeeId()).isEqualTo("E1");
        assertThat(out.getDepartment()).isEqualTo("Dept");
        assertThat(out.getRole()).isEqualTo("ADMIN");
        assertThat(out.getEnabled()).isTrue();
    }

    @Test
    void updateOwnProfile_emailTaken() {
        found(user("1", "USER", true));
        when(repo.existsByEmail("taken@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.updateOwnProfile("1", new UpdateOwnProfileRequest("Name", "taken@x.com")))
            .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateOwnProfile_blankEmailBecomesNull() {
        found(user("1", "USER", true));
        assertThat(service.updateOwnProfile("1", new UpdateOwnProfileRequest("Name", "  ")).getEmail()).isNull();
    }

    @Test
    void updateOwnProfile_sameEmailNotTreatedAsTaken() {
        found(user("1", "USER", true));
        when(repo.existsByEmail("1@x.com")).thenReturn(true);
        assertThat(service.updateOwnProfile("1", new UpdateOwnProfileRequest("Name", "1@x.com")).getEmail())
            .isEqualTo("1@x.com");
    }

    @Test
    void updateOwnProfile_notFound() {
        when(repo.findById("9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateOwnProfile("9", new UpdateOwnProfileRequest("Name", null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- searchUsers ----------------

    private void searchStub() {
        when(repo.search(any(), any(), any(), any())).thenReturn(new PageImpl<User>(List.of()));
    }

    @Test
    void searchUsers_keywordWrappedLowercaseTrimmed() {
        searchStub();
        Pageable p = PageRequest.of(0, 10);
        service.searchUsers("  JoHn ", null, null, p);
        verify(repo).search("%john%", null, null, p);
    }

    @Test
    void searchUsers_blankFiltersAreNull() {
        searchStub();
        Pageable p = PageRequest.of(0, 10);
        service.searchUsers("   ", "", "", p);
        service.searchUsers(null, null, null, p);
        verify(repo, times(2)).search(null, null, null, p);
    }

    @Test
    void searchUsers_roleNormalisedAndUnknownIgnored() {
        searchStub();
        Pageable p = PageRequest.of(0, 10);
        service.searchUsers(null, " admin ", null, p);
        verify(repo).search(null, "ADMIN", null, p);
        service.searchUsers(null, "bogus", null, p);
        verify(repo).search(null, null, null, p);
    }

    @Test
    void searchUsers_statusMapping() {
        searchStub();
        Pageable p = PageRequest.of(0, 10);
        service.searchUsers(null, null, "ACTIVE", p);
        verify(repo).search(null, null, true, p);
        service.searchUsers(null, null, "inactive", p);
        verify(repo).search(null, null, false, p);
        service.searchUsers(null, null, "all", p);
        verify(repo).search(null, null, null, p);
    }

    // ---------------- loadUserByUsername ----------------

    @Test
    void loadUser_byUsername() {
        User u = user("1", "USER", true);
        when(repo.findByUsername("user1")).thenReturn(Optional.of(u));
        when(repo.findByEmployeeId(any())).thenReturn(Optional.empty());
        UserDetails d = service.loadUserByUsername("user1");
        assertThat(d).isSameAs(u);
    }

    @Test
    void loadUser_byEmployeeId() {
        User u = user("1", "USER", true);
        when(repo.findByUsername("E1")).thenReturn(Optional.empty());
        when(repo.findByEmployeeId("E1")).thenReturn(Optional.of(u));
        assertThat(service.loadUserByUsername("E1")).isSameAs(u);
    }

    @Test
    void loadUser_unknown() {
        when(repo.findByUsername("x")).thenReturn(Optional.empty());
        when(repo.findByEmployeeId("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("x")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUser_disabled() {
        User u = user("1", "USER", false);
        when(repo.findByUsername("user1")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.loadUserByUsername("user1")).isInstanceOf(UsernameNotFoundException.class);
    }
}
