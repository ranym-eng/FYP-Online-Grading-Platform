package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.user.EvaluatorProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "project_evaluator_assignments")
public class ProjectEvaluatorAssignment extends BaseEntity {
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne(optional = false)
    private EvaluatorProfile evaluator;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType evaluationType;
    private boolean active = true;
}
