package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.Accident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccidentRepository extends JpaRepository<Accident, String> {
    List<Accident> findByStatus(Accident.AccidentStatus status);
    List<Accident> findBySeverity(Accident.SeverityLevel severity);
    Page<Accident> findAll(Pageable pageable);
    Page<Accident> findByStatus(Accident.AccidentStatus status, Pageable pageable);
    Page<Accident> findBySeverity(Accident.SeverityLevel severity, Pageable pageable);
    Accident findByCode(String code);
}
