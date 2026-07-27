package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.HseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HseDocumentRepository extends JpaRepository<HseDocument, Long> {

    List<HseDocument> findByModuleOrderByUploadedAtDesc(HseDocument.Module module);

    Optional<HseDocument> findByCode(String code);

    long countByModule(HseDocument.Module module);

    boolean existsByCode(String code);

    Optional<HseDocument> findTopByCodeStartingWithOrderByCodeDesc(String prefix);
}
