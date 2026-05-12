package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Observation entity
 * Handles database operations for observations (near misses, unsafe conditions, unsafe acts)
 */
@Repository
public interface ObservationRepository extends JpaRepository<Observation, String> {
    
    // Primary queries use id (inherited from JpaRepository: findById)
    
    // Status queries
    List<Observation> findByStatus(Observation.Status status);
    Page<Observation> findByStatus(Observation.Status status, Pageable pageable);
    
    // Category queries
    List<Observation> findByCategory(String category);
    Page<Observation> findByCategory(String category, Pageable pageable);
    
    // Type queries
    List<Observation> findByType(Observation.Type type);
    Page<Observation> findByType(Observation.Type type, Pageable pageable);
    
    // Mitigation level queries
    List<Observation> findByMitigation(String mitigation);
    Page<Observation> findByMitigation(String mitigation, Pageable pageable);
    
    // Code-based lookup for REST API → id conversion with JOIN FETCH for files
    @Query("SELECT DISTINCT o FROM Observation o LEFT JOIN FETCH o.files WHERE o.code = :code")
    Observation findByCodeWithFiles(@Param("code") String code);
    
    // Simple code lookup
    Observation findByCode(String code);
    
    // Get the last observation by code in descending order (numeric sort)
    @Query(value = "SELECT * FROM observations ORDER BY CAST(SUBSTRING(code, 5) AS UNSIGNED) DESC LIMIT 1", nativeQuery = true)
    Observation findLastObservation();

    // Count by status (derived query)
    long countByStatus(Observation.Status status);

    // Monthly count by status for a given year: returns [month (1-12), count]
    @Query("SELECT MONTH(o.observationDate), COUNT(o) FROM Observation o WHERE YEAR(o.observationDate) = :year AND o.status = :status GROUP BY MONTH(o.observationDate)")
    List<Object[]> countByYearAndStatus(@Param("year") int year, @Param("status") Observation.Status status);
}
