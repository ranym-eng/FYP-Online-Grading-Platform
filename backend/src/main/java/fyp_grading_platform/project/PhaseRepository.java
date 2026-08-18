package fyp_grading_platform.project;

import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.PhaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseRepository extends JpaRepository<Phase, UUID> {
    List<Phase> findByAcademicYear(String academicYear);
    List<Phase> findByStatus(PhaseStatus status);
    Optional<Phase> findByAcademicYearAndType(String academicYear, PhaseType type);
}
