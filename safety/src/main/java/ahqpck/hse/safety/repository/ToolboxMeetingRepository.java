package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolboxMeetingRepository extends JpaRepository<ToolboxMeeting, String> {

    Optional<ToolboxMeeting> findByCode(String code);

    List<ToolboxMeeting> findByStatus(ToolboxMeeting.MeetingStatus status);

    List<ToolboxMeeting> findByMeetingDateBetween(LocalDate from, LocalDate to);

    List<ToolboxMeeting> findBySupervisorEmployeeId(String employeeId);

    boolean existsByCode(String code);
}
