package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rubric_criteria")
public class RubricCriterion extends BaseEntity {
    @ManyToOne(optional = false)
    private EvaluationFormTemplate formTemplate;
    @Column(nullable = false)
    private String title;
    @Column(length = 2000)
    private String description;
    private double maxScore = 100.0;
    private double weight = 1.0;
    private int displayOrder;
    private boolean required = true;
}
