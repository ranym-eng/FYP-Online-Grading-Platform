package fyp_grading_platform.grading;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "grades")
public class Grade extends BaseEntity {
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne(optional = false)
    private Phase phase;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseType phaseType;
    private double rawScore;
    private double weightedScore;
    private double finalScore;
    private boolean published;
}
