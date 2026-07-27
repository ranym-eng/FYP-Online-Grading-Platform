package fyp_grading_platform.grading;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findByProjectId(UUID projectId);
    Optional<Grade> findByProjectIdAndPhaseId(UUID projectId, UUID phaseId);
}
