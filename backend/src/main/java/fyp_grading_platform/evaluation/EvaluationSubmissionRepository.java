package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationSubmissionRepository extends JpaRepository<EvaluationSubmission, UUID> {
    List<EvaluationSubmission> findByProjectId(UUID projectId);
    List<EvaluationSubmission> findByEvaluatorId(UUID evaluatorId);
    List<EvaluationSubmission> findByProjectIdAndPhaseId(UUID projectId, UUID phaseId);
    List<EvaluationSubmission> findByProjectIdAndEvaluationType(UUID projectId, EvaluationType evaluationType);
    Optional<EvaluationSubmission> findFirstByProjectIdAndPhaseIdAndEvaluatorIdAndEvaluationTypeOrderByCreatedAtDesc(
            UUID projectId,
            UUID phaseId,
            UUID evaluatorId,
            EvaluationType evaluationType
    );
    boolean existsByProjectIdAndPhaseIdAndEvaluationTypeAndLockedTrue(UUID projectId, UUID phaseId, EvaluationType evaluationType);
}
