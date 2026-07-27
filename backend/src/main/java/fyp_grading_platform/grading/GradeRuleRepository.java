package fyp_grading_platform.grading;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRuleRepository extends JpaRepository<GradeRule, UUID> {
    List<GradeRule> findByPhaseTypeAndActiveTrue(PhaseType phaseType);
    Optional<GradeRule> findByPhaseTypeAndEvaluationTypeAndActiveTrue(PhaseType phaseType, EvaluationType evaluationType);
}
