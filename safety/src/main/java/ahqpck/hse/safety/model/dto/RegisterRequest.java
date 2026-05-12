package ahqpck.hse.safety.model.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Email should be valid")
    private String email;  // Optional field

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    private String username;

    private String password;

    private String confirmPassword;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Role is required")
    private String role;

    @Builder.Default
    private Boolean agreeToTerms = false;

    @Builder.Default
    private Boolean noLoginRequired = false;
}
