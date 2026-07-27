package fyp_grading_platform.notification;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final EmailNotificationRepository repository;
    private final EmailDeliveryService delivery;
    private final DeadlineReminderService reminders;
    private final CurrentUserService currentUsers;

    public NotificationController(
            EmailNotificationRepository repository,
            EmailDeliveryService delivery,
            DeadlineReminderService reminders,
            CurrentUserService currentUsers
    ) {
        this.repository = repository;
        this.delivery = delivery;
        this.reminders = reminders;
        this.currentUsers = currentUsers;
    }

    @PostMapping("/email")
    ApiResponse<?> email(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EmailRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok(
                "Email notification processed",
                delivery.send(
                        request.recipient(),
                        request.subject(),
                        request.body(),
                        request.attachmentPath()
                )
        );
    }

    @GetMapping
    ApiResponse<?> all(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Notifications", repository.findAll());
    }

    @GetMapping("/me")
    ApiResponse<?> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User user = currentUsers.requireUser(authorization);
        reminders.generateDueReminders();
        return ApiResponse.ok(
                "My notifications",
                repository.findByRecipientIgnoreCaseOrderByCreatedAtDesc(user.getEmail())
        );
    }

    @GetMapping("/me/unread-count")
    ApiResponse<?> unreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok(
                "Unread notification count",
                Map.of("count", repository.countByRecipientIgnoreCaseAndReadAtIsNull(user.getEmail()))
        );
    }

    @PatchMapping("/{id}/read")
    ApiResponse<?> markRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        User user = currentUsers.requireUser(authorization);
        EmailNotification notification = ownNotification(id, user);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = repository.save(notification);
        }
        return ApiResponse.ok("Notification marked as read", notification);
    }

    @PatchMapping("/me/read-all")
    ApiResponse<?> markAllRead(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User user = currentUsers.requireUser(authorization);
        List<EmailNotification> notifications = repository
                .findByRecipientIgnoreCaseOrderByCreatedAtDesc(user.getEmail());
        LocalDateTime now = LocalDateTime.now();
        notifications.stream()
                .filter(notification -> notification.getReadAt() == null)
                .forEach(notification -> notification.setReadAt(now));
        repository.saveAll(notifications);
        return ApiResponse.ok("All notifications marked as read", null);
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Notification", ownNotification(id, user));
    }

    @GetMapping("/status/{status}")
    ApiResponse<?> byStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String status
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Notifications", repository.findByStatus(status));
    }

    @PostMapping("/{id}/retry")
    ApiResponse<?> retry(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        EmailNotification notification = repository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        return ApiResponse.ok("Notification retry processed", delivery.retry(notification));
    }

    @PostMapping("/reminders/evaluation-deadline")
    ApiResponse<?> reminders(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok(
                "Deadline reminders triggered",
                Map.of("created", reminders.generateDueReminders())
        );
    }

    private EmailNotification ownNotification(UUID id, User user) {
        EmailNotification notification = repository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (notification.getRecipient() == null
                || !notification.getRecipient().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("NOTIFICATION_ACCESS_DENIED", "You cannot access this notification");
        }
        return notification;
    }
}