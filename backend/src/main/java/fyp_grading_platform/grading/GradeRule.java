package fyp_grading_platform.grading;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "grade_rules")
public class GradeRule extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseType phaseType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType evaluationType;
    private double weight;
    private boolean active = true;
}
