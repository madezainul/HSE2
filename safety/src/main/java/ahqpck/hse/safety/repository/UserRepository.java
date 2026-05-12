package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.User;

import org.apache.poi.sl.draw.geom.GuideIf.Op;
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
}
