package fyp_grading_platform.notification;

import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseAccessResponse;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.PhaseWindowService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class DeadlineReminderService {
    private final PhaseRepository phases;
    private final UserRepository users;
    private final InAppNotificationService notifications;
    private final PhaseWindowService phaseWindows;

    public DeadlineReminderService(
            PhaseRepository phases,
            UserRepository users,
            InAppNotificationService notifications,
            PhaseWindowService phaseWindows
    ) {
        this.phases = phases;
        this.users = users;
        this.notifications = notifications;
        this.phaseWindows = phaseWindows;
    }

    @Scheduled(cron = "${app.notifications.deadline-reminders-cron:0 */15 * * * *}")
    @Transactional
    public int generateDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<User> recipients = users.findByStatus(UserStatus.ACTIVE);
        int created = 0;
        for (Phase phase : phases.findAll()) {
            for (User user : recipients) {
                PhaseAccessResponse access = phaseWindows.access(phase, user);
                if (!access.allowed() || access.effectiveDeadline() == null) continue;
                LocalDateTime effectiveDeadline = access.effectiveDeadline();
                ReminderWindow window = reminderWindow(effectiveDeadline, now);
                if (window == null) continue;
                String key = "PHASE_DEADLINE_" + window.code() + ":" + phase.getId() + ":"
                        + user.getId() + ":" + effectiveDeadline.toEpochSecond(ZoneOffset.UTC);
                boolean existed = notifications.existsByDeduplicationKey(key);
                notifications.create(
                        user,
                        window.subject(),
                        window.message(phase, effectiveDeadline),
                        "DEADLINE",
                        window.severity(),
                        key,
                        evaluationView(user)
                );
                if (!existed) created++;
            }
        }
        return created;
    }

    private ReminderWindow reminderWindow(LocalDateTime effectiveDeadline, LocalDateTime now) {
        Duration remaining = Duration.between(now, effectiveDeadline);
        if (remaining.isNegative() || remaining.isZero() || remaining.compareTo(Duration.ofHours(24)) > 0) {
            return null;
        }
        if (remaining.compareTo(Duration.ofHours(12)) <= 0) {
            return new ReminderWindow(
                    "12H",
                    "Échéance dans moins de 12 heures",
                    "URGENT",
                    "La phase %s arrive à échéance dans moins de 12 heures, le %s."
            );
        }
        return new ReminderWindow(
                "24H",
                "Échéance dans moins d’un jour",
                "WARNING",
                "La phase %s arrive à échéance dans moins d’un jour, le %s."
        );
    }

    private String evaluationView(User user) {
        return switch (user.getRole()) {
            case SUPERVISOR, FACULTY_EVALUATOR, INDUSTRY_REPRESENTATIVE -> "evaluations";
            default -> "dashboard";
        };
    }

    private record ReminderWindow(String code, String subject, String severity, String bodyPattern) {
        String message(Phase phase, LocalDateTime effectiveDeadline) {
            return bodyPattern.formatted(phase.getName(), effectiveDeadline);
        }
    }
}
