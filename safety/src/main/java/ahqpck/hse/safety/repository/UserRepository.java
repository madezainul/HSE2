package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.User;

import org.apache.poi.sl.draw.geom.GuideIf.Op;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeId(String employeeId);

    Optional<User> findById(String id);

    @Query("SELECT u FROM User u WHERE u.username = :credential OR u.employeeId = :credential")
    Optional<User> findByUsernameOrEmployeeId(@Param("credential") String credential);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    // Roles are stored in mixed case ("admin" from older users, "ADMIN" from newer ones)
    long countByRoleIgnoreCaseAndEnabledTrue(String role);

    /**
     * User list search. Every filter is optional (pass null to skip it).
     * @param keyword lower-case text wrapped in % signs, matched against name, employee ID, username and email
     * @param role upper-case role name
     * @param enabled true for active users, false for inactive ones
     */
    @Query("SELECT u FROM User u WHERE "
         + "(:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(u.employeeId) LIKE :keyword "
         + "   OR LOWER(u.username) LIKE :keyword OR LOWER(COALESCE(u.email, '')) LIKE :keyword) "
         + "AND (:role IS NULL OR UPPER(u.role) = :role) "
         + "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") String role,
                      @Param("enabled") Boolean enabled,
                      Pageable pageable);
}
