package fyp_grading_platform.reporting;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report extends BaseEntity {
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne
    private Phase phase;
    private String filePath;
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private String recipientEmail;
    @Column(length = 8000)
    private String contentSnapshot;
}
