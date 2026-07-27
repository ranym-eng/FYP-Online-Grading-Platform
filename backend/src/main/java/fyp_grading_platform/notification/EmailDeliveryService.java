package fyp_grading_platform.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailDeliveryService {
    private final EmailNotificationRepository notifications;
    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailDeliveryService(
            EmailNotificationRepository notifications,
            JavaMailSender mailSender,
            @Value("${spring.mail.from:no-reply@squ.edu.om}") String senderAddress
    ) {
        this.notifications = notifications;
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    public EmailNotification send(String recipient, String subject, String body, String attachmentPath) {
        EmailNotification notification = new EmailNotification();
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setAttachmentPath(attachmentPath);
        notification.setStatus("PENDING");
        notification = notifications.save(notification);
        return deliver(notification);
    }

    public EmailNotification retry(EmailNotification notification) {
        notification.setStatus("PENDING");
        notification.setFailureReason(null);
        return deliver(notification);
    }

    private EmailNotification deliver(EmailNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderAddress);
            message.setTo(notification.getRecipient());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody() == null ? "" : notification.getBody());
            mailSender.send(message);
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } catch (MailException exception) {
            // The in-app notification remains available even when SMTP is temporarily unavailable.
            notification.setStatus("FAILED");
            notification.setFailureReason(limit(exception.getMessage(), 1000));
        }
        return notifications.save(notification);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
