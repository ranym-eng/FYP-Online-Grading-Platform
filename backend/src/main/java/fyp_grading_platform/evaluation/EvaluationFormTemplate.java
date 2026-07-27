package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "evaluation_form_templates")
public class EvaluationFormTemplate extends BaseEntity {
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType evaluationType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseType phaseType;
    @Column(length = 2000)
    private String description;
    private double totalWeight = 100.0;
    private boolean active = true;
}
