package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectEvaluatorAssignmentRepository extends JpaRepository<ProjectEvaluatorAssignment, UUID> {
    List<ProjectEvaluatorAssignment> findByProjectIdAndActiveTrue(UUID projectId);
    List<ProjectEvaluatorAssignment> findByEvaluatorIdAndActiveTrue(UUID evaluatorId);
    boolean existsByProjectIdAndEvaluatorIdAndActiveTrue(UUID projectId, UUID evaluatorId);
    List<ProjectEvaluatorAssignment> findByProjectIdAndEvaluationTypeAndActiveTrue(UUID projectId, EvaluationType evaluationType);
    Optional<ProjectEvaluatorAssignment> findByProjectIdAndEvaluatorIdAndEvaluationType(
            UUID projectId,
            UUID evaluatorId,
            EvaluationType evaluationType
    );
    boolean existsByProjectIdAndEvaluatorIdAndEvaluationTypeAndActiveTrue(UUID projectId, UUID evaluatorId, EvaluationType evaluationType);
}
