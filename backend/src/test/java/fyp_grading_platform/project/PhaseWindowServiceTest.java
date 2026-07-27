package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseWindowServiceTest {
    private PhaseExtensionRequestRepository requests;
    private PhaseWindowService service;
    private Phase phase;
    private User user;

    @BeforeEach
    void setUp() {
        requests = mock(PhaseExtensionRequestRepository.class);
        service = new PhaseWindowService(requests);
        phase = new Phase();
        phase.setId(UUID.randomUUID());
        phase.setStatus(PhaseStatus.OPEN);
        phase.setStartDate(LocalDateTime.now().minusDays(2));
        phase.setDeadline(LocalDateTime.now().plusDays(1));
        user = new User();
        user.setId(UUID.randomUUID());
    }

    @Test
    void allowsEvaluationInsideOpenPhaseWindow() {
        when(requests.findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                phase.getId(), user.getId(), ExtensionRequestStatus.APPROVED)).thenReturn(Optional.empty());

        PhaseAccessResponse access = service.access(phase, user);

        assertTrue(access.allowed());
        assertEquals("EVALUATION_ALLOWED", access.reasonCode());
        assertFalse(access.personalExtension());
    }

    @Test
    void blocksEvaluationAfterDeadline() {
        phase.setDeadline(LocalDateTime.now().minusMinutes(1));
        when(requests.findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                phase.getId(), user.getId(), ExtensionRequestStatus.APPROVED)).thenReturn(Optional.empty());

        PhaseAccessResponse access = service.access(phase, user);

        assertFalse(access.allowed());
        assertEquals("PHASE_DEADLINE_EXPIRED", access.reasonCode());
        BusinessException exception = assertThrows(BusinessException.class, () -> service.assertEvaluationAllowed(phase, user));
        assertEquals("PHASE_DEADLINE_EXPIRED", exception.getErrorCode());
    }

    @Test
    void approvedPersonalExtensionReopensEvaluationOnlyForRequester() {
        phase.setStatus(PhaseStatus.CLOSED);
        phase.setDeadline(LocalDateTime.now().minusHours(2));
        PhaseExtensionRequest extension = new PhaseExtensionRequest();
        extension.setExtendedDeadline(LocalDateTime.now().plusDays(1));
        when(requests.findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                phase.getId(), user.getId(), ExtensionRequestStatus.APPROVED)).thenReturn(Optional.of(extension));

        PhaseAccessResponse access = service.access(phase, user);

        assertTrue(access.allowed());
        assertTrue(access.personalExtension());
        assertEquals(extension.getExtendedDeadline(), access.effectiveDeadline());
    }
}
