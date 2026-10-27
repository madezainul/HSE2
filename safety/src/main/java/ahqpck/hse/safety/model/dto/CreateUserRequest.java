package ahqpck.hse.safety.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO used by an administrator to create a new user.
 * Email is optional for now; it will be used for invitations and password reset once email is set up.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Employee ID is required")
    @Size(min = 2, max = 50, message = "Employee ID must be between 2 and 50 characters")
    private String employeeId;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Username may only contain letters, digits, dot, dash and underscore")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department must be at most 50 characters")
    private String department;

    @NotBlank(message = "Role is required")
    private String role;

    /** True for people who are recorded in the system but never sign in (no password needed). */
    @Builder.Default
    private Boolean noLoginRequired = false;

    @ToString.Exclude
    private String password;

    @ToString.Exclude
    private String confirmPassword;
}
