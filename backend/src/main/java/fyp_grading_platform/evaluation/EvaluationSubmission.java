package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.SubmissionStatus;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.user.EvaluatorProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "evaluation_submissions")
public class EvaluationSubmission extends BaseEntity {
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne(optional = false)
    private Phase phase;
    @ManyToOne(optional = false)
    private EvaluationFormTemplate formTemplate;
    @ManyToOne(optional = false)
    private EvaluatorProfile evaluator;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType evaluationType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.DRAFT;
    private boolean locked;
    private LocalDateTime draftSavedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime lockedAt;
    private double totalScore;
    @Column(columnDefinition = "text")
    private String scorePayload;
    private Integer requiredScoreCount = 0;
    private Integer completedScoreCount = 0;
    @Column(length = 4000)
    private String generalComment;
}
