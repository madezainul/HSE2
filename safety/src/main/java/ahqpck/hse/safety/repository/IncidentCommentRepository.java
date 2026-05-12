package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, String> {
    List<IncidentComment> findByIncidentId(String incidentId);
}
