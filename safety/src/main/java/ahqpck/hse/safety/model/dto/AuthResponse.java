package ahqpck.hse.safety.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Boolean success;

    private String message;

    private String userId;  // UUID Base64 encoded ID

    private String username;

    private String email;

    private String employeeId;
}
