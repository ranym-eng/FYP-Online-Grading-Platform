package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    @Override
    @EntityGraph(attributePaths = {"project", "project.track", "students"})
    List<Team> findAll();

    @Override
    @EntityGraph(attributePaths = {"project", "project.track", "students"})
    Optional<Team> findById(UUID id);

    @EntityGraph(attributePaths = {"project", "project.track", "students"})
    Optional<Team> findByProjectId(UUID projectId);

    @EntityGraph(attributePaths = {"project", "project.track", "students"})
    List<Team> findByAcademicYear(String academicYear);
}
