package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.ToolboxMeetingFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolboxMeetingFileRepository extends JpaRepository<ToolboxMeetingFile, String> {

    List<ToolboxMeetingFile> findByToolboxMeetingId(String toolboxMeetingId);

    List<ToolboxMeetingFile> findByToolboxMeetingIdAndFileType(String toolboxMeetingId, ToolboxMeetingFile.FileType fileType);
}
