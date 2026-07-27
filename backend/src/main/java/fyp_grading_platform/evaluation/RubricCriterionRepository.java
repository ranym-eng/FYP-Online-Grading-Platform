package fyp_grading_platform.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {
    List<RubricCriterion> findByFormTemplateIdOrderByDisplayOrderAsc(UUID formTemplateId);
}
