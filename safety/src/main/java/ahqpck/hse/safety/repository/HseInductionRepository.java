package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseInduction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HseInductionRepository extends JpaRepository<HseInduction, String> {
    HseInduction findByCode(String code);

    Page<HseInduction> findAll(Pageable pageable);
}
