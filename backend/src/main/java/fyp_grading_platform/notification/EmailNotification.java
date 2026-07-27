package fyp_grading_platform.notification;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "email_notifications")
public class EmailNotification extends BaseEntity {
    private String recipient;
    private UUID recipientUserId;
    private String subject;
    @Column(length = 8000)
    private String body;
    private String attachmentPath;
    private LocalDateTime sentAt;
    private String status = "PENDING";
    @Column(length = 1000)
    private String failureReason;
    private String category = "EMAIL";
    private String severity = "INFO";
    @Column(unique = true)
    private String deduplicationKey;
    private String actionView;
    private LocalDateTime readAt;
}