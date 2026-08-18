package fyp_grading_platform.grading;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentPhaseGradeRepository extends JpaRepository<StudentPhaseGrade, UUID> {
    @EntityGraph(attributePaths = {"project", "project.track", "phase", "student"})
    List<StudentPhaseGrade> findByProjectIdOrderByStudentStudentNumberAsc(UUID projectId);

    @EntityGraph(attributePaths = {"project", "project.track", "phase", "student"})
    List<StudentPhaseGrade> findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(UUID projectId, UUID phaseId);

    @EntityGraph(attributePaths = {"project", "project.track", "phase", "student"})
    Optional<StudentPhaseGrade> findByProjectIdAndPhaseIdAndStudentId(UUID projectId, UUID phaseId, UUID studentId);

    @EntityGraph(attributePaths = {"project", "project.track", "phase", "student"})
    List<StudentPhaseGrade> findByPhaseIdOrderByProjectProjectNumberAscStudentStudentNumberAsc(UUID phaseId);
}
