package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    // Primary queries use id (inherited from JpaRepository: findById)
    
    // Status/Severity/Type queries
    List<Incident> findByStatus(Incident.IncidentStatus status);
    List<Incident> findBySeverity(Incident.SeverityLevel severity);
    List<Incident> findByType(Incident.Type type);
    // Page<Incident> findAll(Pageable pageable);
    Page<Incident> findByStatus(Incident.IncidentStatus status, Pageable pageable);
    Page<Incident> findBySeverity(Incident.SeverityLevel severity, Pageable pageable);
    Page<Incident> findByType(Incident.Type type, Pageable pageable);
    
    // Code-based lookup for REST API → id conversion with JOIN FETCH for files
    @Query("SELECT DISTINCT i FROM Incident i LEFT JOIN FETCH i.files WHERE i.code = :code")
    Incident findByCodeWithFiles(@Param("code") String code);

    // Simple code lookup
    Incident findByCode(String code);
    
    // Get the last incident by code in descending order (numeric sort)
    @Query(value = "SELECT * FROM incidents ORDER BY CAST(SUBSTRING(code, 5) AS UNSIGNED) DESC LIMIT 1", nativeQuery = true)
    Incident findLastIncident();

    // Count by status (derived query)
    long countByStatus(Incident.IncidentStatus status);

    // Monthly count by type for a given year: returns [month (1-12), count]
    @Query("SELECT MONTH(i.reportDate), COUNT(i) FROM Incident i WHERE YEAR(i.reportDate) = :year AND i.type = :type GROUP BY MONTH(i.reportDate)")
    List<Object[]> countByYearAndType(@Param("year") int year, @Param("type") Incident.Type type);
    
}
