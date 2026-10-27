package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HseCertificationRepository extends JpaRepository<HseCertification, String> {
    HseCertification findByCode(String code);

    // "CERT-" is 5 characters, so the number starts at position 6
    @Query("SELECT MAX(CAST(SUBSTRING(h.code, 6) AS INTEGER)) FROM HseCertification h WHERE h.code LIKE 'CERT-%'")
    Integer getMaxCodeNumber();
}
