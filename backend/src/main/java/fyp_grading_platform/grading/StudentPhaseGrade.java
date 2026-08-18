package fyp_grading_platform.grading;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.user.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "student_phase_grades", uniqueConstraints = @UniqueConstraint(
        name = "uk_student_phase_grade",
        columnNames = {"project_id", "phase_id", "student_id"}
))
public class StudentPhaseGrade extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseType phaseType;

    private Double supervisorScore;
    private Double reportScore;
    private Double oralScore;
    private Double demoScore;

    @Column(nullable = false)
    private double finalScore;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;
}
