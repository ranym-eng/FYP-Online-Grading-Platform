package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationFormTemplateRepository extends JpaRepository<EvaluationFormTemplate, UUID> {
    List<EvaluationFormTemplate> findByEvaluationType(EvaluationType evaluationType);
    List<EvaluationFormTemplate> findByPhaseType(PhaseType phaseType);
    Optional<EvaluationFormTemplate> findFirstByEvaluationTypeAndActiveTrue(EvaluationType evaluationType);
}
