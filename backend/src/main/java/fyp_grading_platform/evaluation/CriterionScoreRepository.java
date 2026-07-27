package fyp_grading_platform.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CriterionScoreRepository extends JpaRepository<CriterionScore, UUID> {
    List<CriterionScore> findBySubmissionId(UUID submissionId);
    void deleteBySubmissionId(UUID submissionId);
}
