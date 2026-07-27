package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByProjectId(UUID projectId);
    List<Report> findByStatus(ReportStatus status);
}
