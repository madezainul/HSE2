package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseInductionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HseInductionFileRepository extends JpaRepository<HseInductionFile, String> {
    List<HseInductionFile> findByHseInductionCode(String hseInductionCode);
}
