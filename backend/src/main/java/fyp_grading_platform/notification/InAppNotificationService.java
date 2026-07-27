package fyp_grading_platform.notification;

import fyp_grading_platform.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InAppNotificationService {
    private final EmailNotificationRepository notifications;

    public InAppNotificationService(EmailNotificationRepository notifications) {
        this.notifications = notifications;
    }

    public boolean existsByDeduplicationKey(String key) {
        return notifications.existsByDeduplicationKey(key);
    }

    @Transactional
    public EmailNotification create(
            User recipient,
            String subject,
            String body,
            String category,
            String severity,
            String deduplicationKey,
            String actionView
    ) {
        if (deduplicationKey != null && notifications.existsByDeduplicationKey(deduplicationKey)) {
            return notifications.findByDeduplicationKey(deduplicationKey).orElseThrow();
        }
        EmailNotification notification = new EmailNotification();
        notification.setRecipient(recipient.getEmail());
        notification.setRecipientUserId(recipient.getId());
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setSeverity(severity);
        notification.setDeduplicationKey(deduplicationKey);
        notification.setActionView(actionView);
        notification.setStatus("IN_APP");
        notification.setSentAt(LocalDateTime.now());
        return notifications.save(notification);
    }
}
