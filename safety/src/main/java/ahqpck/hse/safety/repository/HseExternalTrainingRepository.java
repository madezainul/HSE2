package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseExternalTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HseExternalTrainingRepository extends JpaRepository<HseExternalTraining, String> {
    HseExternalTraining findByCode(String code);

    // "EXT-TRAIN-" is 10 characters, so the number starts at position 11
    @Query("SELECT MAX(CAST(SUBSTRING(h.code, 11) AS INTEGER)) FROM HseExternalTraining h WHERE h.code LIKE 'EXT-TRAIN-%'")
    Integer getMaxCodeNumber();
}
