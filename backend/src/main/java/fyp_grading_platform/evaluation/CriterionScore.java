package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "criterion_scores")
public class CriterionScore extends BaseEntity {
    @ManyToOne(optional = false)
    private EvaluationSubmission submission;
    @ManyToOne(optional = false)
    private RubricCriterion criterion;
    private double score;
    @Column(length = 2000)
    private String comment;
}
