package fyp_grading_platform.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {
    List<EmailNotification> findByStatus(String status);
    List<EmailNotification> findByRecipientIgnoreCaseOrderByCreatedAtDesc(String recipient);
    long countByRecipientIgnoreCaseAndReadAtIsNull(String recipient);
    boolean existsByDeduplicationKey(String deduplicationKey);
    Optional<EmailNotification> findByDeduplicationKey(String deduplicationKey);
}
