package ahqpck.hse.safety.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.User;

/**
 * Data Transfer Object for User entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private String id;
    private String username;
    private String email;
    private String employeeId;
    private String fullName;
    private String department;
    private String role;
    private Boolean enabled;

    /**
     * Convert UserDTO to User entity
     */
    public User toEntity() {
        return User.builder()
                .id(this.id)
                .username(this.username)
                .email(this.email)
                .employeeId(this.employeeId)
                .fullName(this.fullName)
                .department(this.department)
                .role(this.role)
                .enabled(this.enabled != null ? this.enabled : true)
                .build();
    }

    /**
     * Convert User entity to UserDTO
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();
    }
}
