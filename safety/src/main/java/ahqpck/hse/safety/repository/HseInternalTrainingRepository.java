package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseInternalTraining;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HseInternalTrainingRepository extends JpaRepository<HseInternalTraining, Long> {
    HseInternalTraining findByCode(String code);

    Page<HseInternalTraining> findAll(Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(h.code, 11) AS INTEGER)) FROM HseInternalTraining h WHERE h.code LIKE 'INT-TRAIN-%'")
    Integer getMaxCodeNumber();
}
