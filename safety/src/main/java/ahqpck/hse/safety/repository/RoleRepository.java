package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by name.
     * @param name the role name
     * @return Optional containing Role if found
     */
    Optional<Role> findByName(Role.RoleName name);

    /**
     * Check if role exists by name.
     * @param name the role name
     * @return true if role exists
     */
    boolean existsByName(Role.RoleName name);
}
