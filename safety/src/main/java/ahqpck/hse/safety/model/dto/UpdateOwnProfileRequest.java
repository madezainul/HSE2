package ahqpck.hse.safety.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for users editing their own profile.
 * <p>
 * Only name and email can be changed here. Employee ID, department and role are set by an
 * administrator, so they are deliberately not part of this form (a user must not be able to
 * give themselves a different role by posting an extra field).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOwnProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Email(message = "Email should be valid")
    private String email;
}
