package ahqpck.hse.safety.model.mapper;

import ahqpck.hse.safety.model.dto.UserProfileResponse;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.util.Base62Utils;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entity to DTOs and vice versa.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserProfileResponse DTO.
     * Uses the Base62 UUID ID directly.
     * @param user User entity
     * @return UserProfileResponse DTO
     */
    public UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .accountNonExpired(user.getAccountNonExpired())
                .accountNonLocked(user.getAccountNonLocked())
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Convert UserProfileResponse DTO to User entity.
     * @param response UserProfileResponse DTO
     * @return User entity
     */
    public User toEntity(UserProfileResponse response) {
        if (response == null) {
            return null;
        }

        return User.builder()
                .id(response.getId())
                .username(response.getUsername())
                .email(response.getEmail())
                .employeeId(response.getEmployeeId())
                .fullName(response.getFullName())
                .department(response.getDepartment())
                .role(response.getRole())
                .enabled(response.getEnabled())
                .accountNonExpired(response.getAccountNonExpired())
                .accountNonLocked(response.getAccountNonLocked())
                .credentialsNonExpired(response.getCredentialsNonExpired())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
