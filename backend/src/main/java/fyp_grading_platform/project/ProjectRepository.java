package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByProjectNumberIgnoreCase(String projectNumber);
    List<Project> findByTrackId(UUID trackId);
    List<Project> findByAcademicYear(String academicYear);
    List<Project> findByTitleContainingIgnoreCaseOrProjectNumberContainingIgnoreCase(String title, String projectNumber);
}