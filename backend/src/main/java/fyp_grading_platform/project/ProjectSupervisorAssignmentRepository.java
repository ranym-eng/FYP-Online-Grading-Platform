package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSupervisorAssignmentRepository extends JpaRepository<ProjectSupervisorAssignment, UUID> {

    List<ProjectSupervisorAssignment> findAllByProjectIdAndActiveTrue(UUID projectId);
    Optional<ProjectSupervisorAssignment> findByProjectIdAndSupervisorId(UUID projectId, UUID supervisorId);
    List<ProjectSupervisorAssignment> findBySupervisorIdAndActiveTrue(UUID supervisorId);
    boolean existsByProjectIdAndSupervisorIdAndActiveTrue(UUID projectId, UUID supervisorId);
}
