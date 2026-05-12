package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.ObservationFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ObservationFile entity
 * Handles database operations for observation files (images, documents)
 */
@Repository
public interface ObservationFileRepository extends JpaRepository<ObservationFile, String> {
    
    /**
     * Find all files for a specific observation
     * @param observationId the observation ID
     * @return list of observation files
     */
    List<ObservationFile> findByObservationId(String observationId);
    
    /**
     * Find files of a specific type for an observation
     * @param observationId the observation ID
     * @param fileType the file type (IMAGE or DOCUMENT)
     * @return list of observation files matching the type
     */
    List<ObservationFile> findByObservationIdAndFileType(String observationId, ObservationFile.FileType fileType);
    
    /**
     * Delete a file by its path
     * @param filePath the file path
     */
    void deleteByFilePath(String filePath);
}
