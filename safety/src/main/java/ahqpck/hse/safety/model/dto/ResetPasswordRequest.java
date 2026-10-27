package ahqpck.hse.safety.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO used by an administrator to set a new password for another user.
 * Unlike {@link ChangePasswordRequest} it does not ask for the current password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @ToString.Exclude
    @NotBlank(message = "New password is required")
    private String newPassword;

    @ToString.Exclude
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
