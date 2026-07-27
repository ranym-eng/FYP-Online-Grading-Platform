package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByTrackId(UUID trackId);
    List<Project> findByAcademicYear(String academicYear);
    List<Project> findByTitleContainingIgnoreCase(String keyword);
}
