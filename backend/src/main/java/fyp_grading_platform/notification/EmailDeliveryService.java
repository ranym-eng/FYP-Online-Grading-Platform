package fyp_grading_platform.notification;

import fyp_grading_platform.user.User;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
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

    public EmailNotification sendToUser(
            User recipient,
            String subject,
            String body,
            String category,
            String severity,
            String actionView
    ) {
        EmailNotification notification = new EmailNotification();
        notification.setRecipient(recipient.getEmail());
        notification.setRecipientUserId(recipient.getId());
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setSeverity(severity);
        notification.setActionView(actionView);
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
            if (notification.getAttachmentPath() == null || notification.getAttachmentPath().isBlank()) {
                sendSimple(notification);
            } else {
                sendWithAttachment(notification);
            }
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } catch (MailException | MessagingException exception) {
            // The in-app notification remains available even when SMTP is temporarily unavailable.
            notification.setStatus("FAILED");
            notification.setFailureReason(limit(exception.getMessage(), 1000));
        }
        return notifications.save(notification);
    }

    private void sendSimple(EmailNotification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(notification.getRecipient());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody() == null ? "" : notification.getBody());
        mailSender.send(message);
    }

    private void sendWithAttachment(EmailNotification notification) throws MessagingException {
        var message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderAddress);
        helper.setTo(notification.getRecipient());
        helper.setSubject(notification.getSubject());
        helper.setText(notification.getBody() == null ? "" : notification.getBody());
        Path attachment = Path.of(notification.getAttachmentPath()).toAbsolutePath().normalize();
        helper.addAttachment(attachment.getFileName().toString(), new FileSystemResource(attachment));
        mailSender.send(message);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
