package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.PhaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "phases")
public class Phase extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseType type;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String academicYear;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status = PhaseStatus.NOT_STARTED;
}
