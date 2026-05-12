package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.ObservationComment;
import ahqpck.hse.safety.model.entity.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ObservationComment entity
 * Handles database operations for observation comments
 */
@Repository
public interface ObservationCommentRepository extends JpaRepository<ObservationComment, String> {
    
    /**
     * Find all comments for a specific observation
     * @param observation the observation entity
     * @return list of observation comments
     */
    List<ObservationComment> findByObservation(Observation observation);
    
    /**
     * Find all comments for a specific observation ordered by creation date
     * @param observation the observation entity
     * @return list of observation comments ordered by creation date descending
     */
    List<ObservationComment> findByObservationOrderByCreatedAtDesc(Observation observation);
}
