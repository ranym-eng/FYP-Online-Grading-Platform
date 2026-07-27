package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PhaseExtensionService {
    private static final List<UserRole> EVALUATION_ROLES = List.of(
            UserRole.SUPERVISOR,
            UserRole.FACULTY_EVALUATOR,
            UserRole.INDUSTRY_REPRESENTATIVE
    );

    private final PhaseRepository phases;
    private final PhaseExtensionRequestRepository requests;
    private final UserRepository users;
    private final EmailDeliveryService emails;

    public PhaseExtensionService(
            PhaseRepository phases,
            PhaseExtensionRequestRepository requests,
            UserRepository users,
            EmailDeliveryService emails
    ) {
        this.phases = phases;
        this.requests = requests;
        this.users = users;
        this.emails = emails;
    }

    @Transactional
    public PhaseExtensionRequest create(User requester, PhaseExtensionCreateRequest input) {
        if (!EVALUATION_ROLES.contains(requester.getRole())) {
            throw new BusinessException("ROLE_NOT_ALLOWED", "Only an evaluator can request an evaluation extension");
        }
        Phase phase = phases.findById(input.phaseId())
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        LocalDateTime now = LocalDateTime.now();
        if (phase.getDeadline() == null) {
            throw new BusinessException("PHASE_DEADLINE_MISSING", "The administrator has not configured the phase deadline");
        }
        if (now.isBefore(phase.getDeadline())) {
            throw new BusinessException("DEADLINE_NOT_EXPIRED", "An extension can be requested only after the phase deadline");
        }
        if (input.requestedDeadline() != null && !input.requestedDeadline().isAfter(phase.getDeadline())) {
            throw new BusinessException("INVALID_REQUESTED_DEADLINE", "The requested deadline must be after the current phase deadline");
        }
        var activeExtension = requests.findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                phase.getId(),
                requester.getId(),
                ExtensionRequestStatus.APPROVED
        );
        if (activeExtension.isPresent()
                && activeExtension.get().getExtendedDeadline() != null
                && activeExtension.get().getExtendedDeadline().isAfter(now)) {
            throw new BusinessException(
                    "EXTENSION_ALREADY_ACTIVE",
                    "An approved extension is already active for this phase"
            );
        }
        if (requests.existsByPhaseIdAndRequesterIdAndStatus(phase.getId(), requester.getId(), ExtensionRequestStatus.PENDING)) {
            throw new BusinessException("EXTENSION_ALREADY_PENDING", "An extension request is already pending for this phase");
        }

        PhaseExtensionRequest extension = new PhaseExtensionRequest();
        extension.setPhase(phase);
        extension.setRequester(requester);
        extension.setReason(input.reason().trim());
        extension.setRequestedDeadline(input.requestedDeadline());
        extension.setRequestedAt(now);
        extension.setStatus(ExtensionRequestStatus.PENDING);
        PhaseExtensionRequest saved = requests.save(extension);
        notifyAdministrators(saved);
        return saved;
    }

    public List<PhaseExtensionRequest> all(ExtensionRequestStatus status) {
        return status == null
                ? requests.findAllByOrderByRequestedAtDesc()
                : requests.findByStatusOrderByRequestedAtDesc(status);
    }

    public List<PhaseExtensionRequest> mine(User requester) {
        return requests.findByRequesterIdOrderByRequestedAtDesc(requester.getId());
    }

    @Transactional
    public PhaseExtensionRequest approve(UUID id, PhaseExtensionDecisionRequest decision, User administrator) {
        PhaseExtensionRequest extension = pending(id);
        LocalDateTime deadline = decision.extendedDeadline();
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new BusinessException("INVALID_EXTENDED_DEADLINE", "The extended deadline must be in the future");
        }
        if (extension.getPhase().getDeadline() != null && !deadline.isAfter(extension.getPhase().getDeadline())) {
            throw new BusinessException("INVALID_EXTENDED_DEADLINE", "The extended deadline must be after the phase deadline");
        }
        extension.setStatus(ExtensionRequestStatus.APPROVED);
        extension.setExtendedDeadline(deadline);
        extension.setAdminComment(decision.adminComment());
        extension.setReviewedAt(LocalDateTime.now());
        extension.setReviewedBy(administrator);
        PhaseExtensionRequest saved = requests.save(extension);
        notifyRequester(saved, true);
        return saved;
    }

    @Transactional
    public PhaseExtensionRequest reject(UUID id, PhaseExtensionDecisionRequest decision, User administrator) {
        PhaseExtensionRequest extension = pending(id);
        extension.setStatus(ExtensionRequestStatus.REJECTED);
        extension.setExtendedDeadline(null);
        extension.setAdminComment(decision.adminComment());
        extension.setReviewedAt(LocalDateTime.now());
        extension.setReviewedBy(administrator);
        PhaseExtensionRequest saved = requests.save(extension);
        notifyRequester(saved, false);
        return saved;
    }

    private PhaseExtensionRequest pending(UUID id) {
        PhaseExtensionRequest extension = requests.findById(id)
                .orElseThrow(() -> new BusinessException("EXTENSION_REQUEST_NOT_FOUND", "Extension request not found"));
        if (extension.getStatus() != ExtensionRequestStatus.PENDING) {
            throw new BusinessException("EXTENSION_ALREADY_REVIEWED", "This extension request has already been reviewed");
        }
        return extension;
    }

    private void notifyAdministrators(PhaseExtensionRequest extension) {
        List<User> administrators = users.findByRole(UserRole.ADMIN);
        String subject = "[FYP] Evaluation deadline extension request";
        String body = """
                A new evaluation deadline extension request has been submitted.

                Requester: %s
                Email: %s
                Phase: %s
                Current deadline: %s
                Requested deadline: %s
                Reason: %s

                Please review this request in the FYP Grading Platform.
                """.formatted(
                extension.getRequester().getFullName(),
                extension.getRequester().getEmail(),
                extension.getPhase().getName(),
                extension.getPhase().getDeadline(),
                extension.getRequestedDeadline() == null ? "Not specified" : extension.getRequestedDeadline(),
                extension.getReason()
        );
        administrators.forEach(admin -> emails.send(admin.getEmail(), subject, body, null));
    }

    private void notifyRequester(PhaseExtensionRequest extension, boolean approved) {
        String subject = approved
                ? "[FYP] Extension request approved"
                : "[FYP] Extension request rejected";
        String body = approved
                ? "Your extension request for phase %s has been approved until %s.%nAdministrator comment: %s"
                    .formatted(extension.getPhase().getName(), extension.getExtendedDeadline(), comment(extension))
                : "Your extension request for phase %s has been rejected.%nAdministrator comment: %s"
                    .formatted(extension.getPhase().getName(), comment(extension));
        emails.send(extension.getRequester().getEmail(), subject, body, null);
    }

    private String comment(PhaseExtensionRequest extension) {
        return extension.getAdminComment() == null || extension.getAdminComment().isBlank()
                ? "No comment"
                : extension.getAdminComment();
    }
}
