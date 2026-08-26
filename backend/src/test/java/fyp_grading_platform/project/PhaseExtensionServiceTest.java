package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhaseExtensionServiceTest {
    @Mock PhaseRepository phases;
    @Mock PhaseExtensionRequestRepository requests;
    @Mock UserRepository users;
    @Mock EmailDeliveryService emails;

    private PhaseExtensionService service;
    private Phase phase;
    private User evaluator;

    @BeforeEach
    void setUp() {
        service = new PhaseExtensionService(phases, requests, users, emails);
        phase = new Phase();
        phase.setId(UUID.randomUUID());
        phase.setName("FYP I");
        phase.setDeadline(LocalDateTime.now().minusHours(2));
        evaluator = new User();
        evaluator.setId(UUID.randomUUID());
        evaluator.setFullName("Example Evaluator");
        evaluator.setEmail("evaluator@squ.edu.om");
        evaluator.setRole(UserRole.FACULTY_EVALUATOR);
    }

    @Test
    void evaluatorSubmitsOnlyPhaseAndReason() {
        User administrator = new User();
        administrator.setId(UUID.randomUUID());
        administrator.setEmail("admin@squ.edu.om");
        administrator.setRole(UserRole.ADMIN);
        when(phases.findById(phase.getId())).thenReturn(Optional.of(phase));
        when(requests.findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                phase.getId(), evaluator.getId(), ExtensionRequestStatus.APPROVED)).thenReturn(Optional.empty());
        when(requests.existsByPhaseIdAndRequesterIdAndStatus(
                phase.getId(), evaluator.getId(), ExtensionRequestStatus.PENDING)).thenReturn(false);
        when(requests.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findByRole(UserRole.ADMIN)).thenReturn(List.of(administrator));

        PhaseExtensionRequest created = service.create(
                evaluator,
                new PhaseExtensionCreateRequest(phase.getId(), "Unexpected technical issue")
        );

        assertEquals(ExtensionRequestStatus.PENDING, created.getStatus());
        assertEquals("Unexpected technical issue", created.getReason());
        assertNull(created.getExtendedDeadline());
        verify(emails).sendToUser(
                eq(administrator),
                eq("[FYP] Evaluation deadline extension request"),
                org.mockito.ArgumentMatchers.contains("Unexpected technical issue"),
                eq("EXTENSION"),
                eq("WARNING"),
                eq("extensions")
        );
    }

    @Test
    void onlyAdministratorDecisionSetsTheNewDeadline() {
        User administrator = new User();
        administrator.setId(UUID.randomUUID());
        administrator.setEmail("admin@squ.edu.om");
        administrator.setRole(UserRole.ADMIN);
        PhaseExtensionRequest pending = new PhaseExtensionRequest();
        pending.setId(UUID.randomUUID());
        pending.setPhase(phase);
        pending.setRequester(evaluator);
        pending.setStatus(ExtensionRequestStatus.PENDING);
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(1);
        when(requests.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(requests.save(pending)).thenReturn(pending);

        PhaseExtensionRequest approved = service.approve(
                pending.getId(),
                new PhaseExtensionDecisionRequest(newDeadline, "Approved for this evaluator"),
                administrator
        );

        assertEquals(newDeadline, approved.getExtendedDeadline());
        assertEquals(ExtensionRequestStatus.APPROVED, approved.getStatus());
        assertEquals(administrator, approved.getReviewedBy());
        verify(emails).sendToUser(
                eq(evaluator),
                eq("[FYP] Extension request approved"),
                org.mockito.ArgumentMatchers.contains(newDeadline.toString()),
                eq("EXTENSION"),
                eq("INFO"),
                eq("extensions")
        );
    }
}
