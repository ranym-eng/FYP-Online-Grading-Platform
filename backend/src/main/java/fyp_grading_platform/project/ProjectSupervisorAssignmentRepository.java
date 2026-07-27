package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSupervisorAssignmentRepository extends JpaRepository<ProjectSupervisorAssignment, UUID> {
    Optional<ProjectSupervisorAssignment> findByProjectIdAndActiveTrue(UUID projectId);
    List<ProjectSupervisorAssignment> findBySupervisorIdAndActiveTrue(UUID supervisorId);
}
