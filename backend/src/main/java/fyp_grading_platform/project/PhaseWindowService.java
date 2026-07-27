package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PhaseWindowService {
    private final PhaseExtensionRequestRepository extensionRequests;

    public PhaseWindowService(PhaseExtensionRequestRepository extensionRequests) {
        this.extensionRequests = extensionRequests;
    }

    public PhaseAccessResponse access(Phase phase, User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime phaseDeadline = phase.getDeadline();
        LocalDateTime effectiveDeadline = phaseDeadline;

        var approved = extensionRequests
                .findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
                        phase.getId(), user.getId(), ExtensionRequestStatus.APPROVED)
                .filter(request -> request.getExtendedDeadline() != null);

        if (approved.isPresent() && (effectiveDeadline == null || approved.get().getExtendedDeadline().isAfter(effectiveDeadline))) {
            effectiveDeadline = approved.get().getExtendedDeadline();
        }
        boolean personalExtension = phaseDeadline != null
                && effectiveDeadline != null
                && effectiveDeadline.isAfter(phaseDeadline);

        if (phase.getStatus() == PhaseStatus.ARCHIVED) {
            return denied("PHASE_ARCHIVED", "This phase is archived", phaseDeadline, effectiveDeadline, personalExtension);
        }
        if (phase.getStartDate() == null || phaseDeadline == null) {
            return denied("PHASE_DATES_MISSING", "The administrator must configure the phase dates", phaseDeadline, effectiveDeadline, personalExtension);
        }
        if (now.isBefore(phase.getStartDate()) || phase.getStatus() == PhaseStatus.NOT_STARTED) {
            return denied("PHASE_NOT_STARTED", "The evaluation phase has not started yet", phaseDeadline, effectiveDeadline, personalExtension);
        }

        boolean extensionStillActive = personalExtension && now.isBefore(effectiveDeadline);
        if (phase.getStatus() == PhaseStatus.CLOSED && !extensionStillActive) {
            return denied("PHASE_CLOSED", "The evaluation phase is closed", phaseDeadline, effectiveDeadline, false);
        }
        if (!now.isBefore(effectiveDeadline)) {
            return denied("PHASE_DEADLINE_EXPIRED", "The evaluation deadline has passed", phaseDeadline, effectiveDeadline, personalExtension);
        }
        if (phase.getStatus() != PhaseStatus.OPEN && !extensionStillActive) {
            return denied("PHASE_NOT_OPEN", "The evaluation phase is not open", phaseDeadline, effectiveDeadline, personalExtension);
        }
        return new PhaseAccessResponse(true, "EVALUATION_ALLOWED", "Evaluation is open", phaseDeadline, effectiveDeadline, personalExtension);
    }

    public void assertEvaluationAllowed(Phase phase, User user) {
        PhaseAccessResponse access = access(phase, user);
        if (!access.allowed()) throw new BusinessException(access.reasonCode(), access.message());
    }

    private PhaseAccessResponse denied(
            String code,
            String message,
            LocalDateTime phaseDeadline,
            LocalDateTime effectiveDeadline,
            boolean personalExtension
    ) {
        return new PhaseAccessResponse(false, code, message, phaseDeadline, effectiveDeadline, personalExtension);
    }
}
