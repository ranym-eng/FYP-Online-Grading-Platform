package fyp_grading_platform.grading;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.evaluation.EvaluationSubmission;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GradingService {
    private final GradeRepository grades;
    private final GradeRuleRepository rules;
    private final EvaluationSubmissionRepository submissions;
    private final ProjectRepository projects;
    private final PhaseRepository phases;

    public GradingService(GradeRepository grades, GradeRuleRepository rules, EvaluationSubmissionRepository submissions, ProjectRepository projects, PhaseRepository phases) {
        this.grades = grades; this.rules = rules; this.submissions = submissions; this.projects = projects; this.phases = phases;
    }

    public Grade calculate(UUID projectId, UUID phaseId) {
        var phase = phases.findById(phaseId).orElseThrow();
        PhaseType phaseType = phase.getType();
        List<GradeRule> activeRules = rules.findByPhaseTypeAndActiveTrue(phaseType);
        if (activeRules.isEmpty()) throw new BusinessException("GRADE_RULES_MISSING", "No grade rules configured for phase");
        List<EvaluationSubmission> phaseSubmissions = submissions.findByProjectIdAndPhaseId(projectId, phaseId).stream().filter(EvaluationSubmission::isLocked).toList();
        for (GradeRule rule : activeRules) {
            boolean exists = phaseSubmissions.stream().anyMatch(s -> s.getEvaluationType() == rule.getEvaluationType());
            if (!exists) throw new BusinessException("GRADE_NOT_READY", "Missing required evaluation: " + rule.getEvaluationType());
        }
        double totalWeight = activeRules.stream().mapToDouble(GradeRule::getWeight).sum();
        double weighted = 0;
        for (GradeRule rule : activeRules) {
            EvaluationType type = rule.getEvaluationType();
            double score = phaseSubmissions.stream().filter(s -> s.getEvaluationType() == type).findFirst().orElseThrow().getTotalScore();
            weighted += score * rule.getWeight();
        }
        double finalScore = totalWeight == 0 ? 0 : weighted / totalWeight;
        Grade grade = grades.findByProjectIdAndPhaseId(projectId, phaseId).orElse(new Grade());
        grade.setProject(projects.findById(projectId).orElseThrow());
        grade.setPhase(phase);
        grade.setPhaseType(phaseType);
        grade.setRawScore(finalScore);
        grade.setWeightedScore(finalScore);
        grade.setFinalScore(finalScore);
        return grades.save(grade);
    }
}
