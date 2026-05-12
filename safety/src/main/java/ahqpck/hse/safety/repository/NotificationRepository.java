package ahqpck.hse.safety.repository;

import ahqpck.hse.safety.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByForUserIdOrderByCreatedAtDesc(String forUserId);

    List<Notification> findByForUserIdAndIsReadFalseOrderByCreatedAtDesc(String forUserId);

    long countByForUserIdAndIsReadFalse(String forUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.forUserId = :userId AND n.isRead = false")
    int markAllReadByUserId(@Param("userId") String userId);
}
