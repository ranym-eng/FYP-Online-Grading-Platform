package fyp_grading_platform.notification;

import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseAccessResponse;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.PhaseWindowService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadlineReminderServiceTest {
    @Test
    void createsHalfDayReminderForEveryActiveUser() {
        PhaseRepository phases = mock(PhaseRepository.class);
        UserRepository users = mock(UserRepository.class);
        InAppNotificationService notifications = mock(InAppNotificationService.class);
        PhaseWindowService phaseWindows = mock(PhaseWindowService.class);
        DeadlineReminderService service = new DeadlineReminderService(
                phases,
                users,
                notifications,
                phaseWindows
        );

        Phase phase = new Phase();
        phase.setId(UUID.randomUUID());
        phase.setName("FYP I");
        phase.setType(PhaseType.PHASE_I);
        phase.setAcademicYear("2025-2026");
        phase.setStatus(PhaseStatus.OPEN);
        phase.setDeadline(LocalDateTime.now().plusHours(10));

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());
        evaluator.setEmail("evaluator@squ.edu.om");
        evaluator.setRole(UserRole.FACULTY_EVALUATOR);
        evaluator.setStatus(UserStatus.ACTIVE);

        when(phases.findAll()).thenReturn(List.of(phase));
        when(users.findByStatus(UserStatus.ACTIVE)).thenReturn(List.of(evaluator));
        when(phaseWindows.access(phase, evaluator)).thenReturn(new PhaseAccessResponse(
                true,
                "EVALUATION_ALLOWED",
                "Evaluation is open",
                phase.getDeadline(),
                phase.getDeadline(),
                false
        ));
        when(notifications.existsByDeduplicationKey(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        assertEquals(1, service.generateDueReminders());
        verify(notifications).create(
                eq(evaluator),
                eq("Échéance dans moins de 12 heures"),
                org.mockito.ArgumentMatchers.contains("FYP I"),
                eq("DEADLINE"),
                eq("URGENT"),
                org.mockito.ArgumentMatchers.anyString(),
                eq("evaluations")
        );
    }
}
