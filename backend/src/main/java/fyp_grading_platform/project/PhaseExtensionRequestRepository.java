package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseExtensionRequestRepository extends JpaRepository<PhaseExtensionRequest, UUID> {
    List<PhaseExtensionRequest> findAllByOrderByRequestedAtDesc();
    List<PhaseExtensionRequest> findByRequesterIdOrderByRequestedAtDesc(UUID requesterId);
    List<PhaseExtensionRequest> findByStatusOrderByRequestedAtDesc(ExtensionRequestStatus status);
    boolean existsByPhaseIdAndRequesterIdAndStatus(UUID phaseId, UUID requesterId, ExtensionRequestStatus status);
    Optional<PhaseExtensionRequest> findFirstByPhaseIdAndRequesterIdAndStatusOrderByExtendedDeadlineDesc(
            UUID phaseId,
            UUID requesterId,
            ExtensionRequestStatus status
    );
}
