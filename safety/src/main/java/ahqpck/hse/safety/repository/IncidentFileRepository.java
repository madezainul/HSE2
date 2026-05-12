package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.IncidentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentFileRepository extends JpaRepository<IncidentFile, String> {
    List<IncidentFile> findByIncidentId(String incidentId);
    List<IncidentFile> findByIncidentIdAndFileType(String incidentId, IncidentFile.FileType fileType);
    void deleteByFilePath(String filePath);
}