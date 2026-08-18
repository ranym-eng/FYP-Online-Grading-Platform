package fyp_grading_platform.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.evaluation.EvaluationSheetCalculator;
import fyp_grading_platform.evaluation.EvaluationSubmission;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.user.StudentProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsolidationService {
    private final StudentPhaseGradeRepository studentGrades;
    private final GradeRepository projectGrades;
    private final GradeRuleRepository rules;
    private final EvaluationSubmissionRepository submissions;
    private final ProjectRepository projects;
    private final PhaseRepository phases;
    private final TeamRepository teams;
    private final EvaluationSheetCalculator calculator;
    private final ObjectMapper objectMapper;

    public ConsolidationService(
            StudentPhaseGradeRepository studentGrades,
            GradeRepository projectGrades,
            GradeRuleRepository rules,
            EvaluationSubmissionRepository submissions,
            ProjectRepository projects,
            PhaseRepository phases,
            TeamRepository teams,
            EvaluationSheetCalculator calculator,
            ObjectMapper objectMapper
    ) {
        this.studentGrades = studentGrades;
        this.projectGrades = projectGrades;
        this.rules = rules;
        this.submissions = submissions;
        this.projects = projects;
        this.phases = phases;
        this.teams = teams;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<StudentPhaseGrade> calculate(UUID projectId, UUID phaseId) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        Phase phase = phases.findById(phaseId)
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        Team team = teams.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("TEAM_NOT_FOUND", "The project has no team"));
        if (team.getStudents().isEmpty()) {
            throw new BusinessException("TEAM_HAS_NO_STUDENTS", "The project team has no students");
        }

        List<GradeRule> activeRules = rules.findByPhaseTypeAndActiveTrue(phase.getType());
        if (activeRules.isEmpty()) {
            throw new BusinessException("GRADE_RULES_MISSING", "No grade rules are configured for this phase");
        }

        List<EvaluationSubmission> locked = submissions.findByProjectIdAndPhaseId(projectId, phaseId).stream()
                .filter(EvaluationSubmission::isLocked)
                .toList();
        for (GradeRule rule : activeRules) {
            if (locked.stream().noneMatch(item -> item.getEvaluationType() == rule.getEvaluationType())) {
                throw new BusinessException(
                        "GRADE_NOT_READY",
                        "Missing locked evaluation: " + rule.getEvaluationType()
                );
            }
        }

        Set<UUID> currentStudentIds = team.getStudents().stream().map(StudentProfile::getId).collect(Collectors.toSet());
        studentGrades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(projectId, phaseId).stream()
                .filter(existing -> !currentStudentIds.contains(existing.getStudent().getId()))
                .forEach(studentGrades::delete);

        List<StudentPhaseGrade> calculated = team.getStudents().stream()
                .sorted(java.util.Comparator.comparing(StudentProfile::getStudentNumber))
                .map(student -> calculateStudent(project, phase, student, activeRules, locked))
                .toList();
        studentGrades.saveAll(calculated);
        updateProjectSummary(project, phase, calculated);
        return studentGrades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(projectId, phaseId);
    }

    @Transactional(readOnly = true)
    public List<StudentPhaseGrade> results(UUID projectId, UUID phaseId) {
        return studentGrades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(projectId, phaseId);
    }

    @Transactional(readOnly = true)
    public List<StudentPhaseGrade> resultsByProject(UUID projectId) {
        return studentGrades.findByProjectIdOrderByStudentStudentNumberAsc(projectId);
    }

    @Transactional
    public List<StudentPhaseGrade> publish(UUID projectId, UUID phaseId, boolean published) {
        List<StudentPhaseGrade> values = results(projectId, phaseId);
        if (values.isEmpty()) {
            throw new BusinessException("GRADE_NOT_CALCULATED", "Calculate student grades before publishing them");
        }
        values.forEach(grade -> grade.setPublished(published));
        studentGrades.saveAll(values);
        projectGrades.findByProjectIdAndPhaseId(projectId, phaseId).ifPresent(summary -> {
            summary.setPublished(published);
            projectGrades.save(summary);
        });
        return results(projectId, phaseId);
    }

    private StudentPhaseGrade calculateStudent(
            Project project,
            Phase phase,
            StudentProfile student,
            List<GradeRule> activeRules,
            List<EvaluationSubmission> locked
    ) {
        Map<EvaluationType, Double> averages = new EnumMap<>(EvaluationType.class);
        for (GradeRule rule : activeRules) {
            averages.put(
                    rule.getEvaluationType(),
                    averageForStudent(locked, rule.getEvaluationType(), student.getId().toString())
            );
        }

        double totalWeight = activeRules.stream().mapToDouble(GradeRule::getWeight).sum();
        double weighted = activeRules.stream()
                .mapToDouble(rule -> averages.get(rule.getEvaluationType()) * rule.getWeight())
                .sum();
        double finalScore = totalWeight == 0 ? 0 : weighted / totalWeight;

        StudentPhaseGrade grade = studentGrades
                .findByProjectIdAndPhaseIdAndStudentId(project.getId(), phase.getId(), student.getId())
                .orElseGet(StudentPhaseGrade::new);
        grade.setProject(project);
        grade.setPhase(phase);
        grade.setStudent(student);
        grade.setPhaseType(phase.getType());
        grade.setSupervisorScore(first(averages, EvaluationType.SUPERVISOR_PHASE_I, EvaluationType.SUPERVISOR_PHASE_II));
        grade.setReportScore(first(averages, EvaluationType.REPORT_PHASE_I, EvaluationType.REPORT_PHASE_II));
        grade.setOralScore(first(averages, EvaluationType.ORAL_PHASE_I, EvaluationType.ORAL_PHASE_II));
        grade.setDemoScore(averages.get(EvaluationType.DEMO_DAY_INDUSTRY));
        grade.setFinalScore(round(finalScore));
        grade.setCalculatedAt(LocalDateTime.now());
        return grade;
    }

    private double averageForStudent(
            List<EvaluationSubmission> locked,
            EvaluationType type,
            String studentId
    ) {
        List<Double> values = locked.stream()
                .filter(submission -> submission.getEvaluationType() == type)
                .map(submission -> scoreForStudent(submission, studentId))
                .toList();
        if (values.isEmpty()) {
            throw new BusinessException("GRADE_NOT_READY", "Missing locked evaluation: " + type);
        }
        return round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private double scoreForStudent(EvaluationSubmission submission, String studentId) {
        if (submission.getScorePayload() == null || submission.getScorePayload().isBlank()) {
            return submission.getTotalScore();
        }
        try {
            Map<String, Double> scores = objectMapper.readValue(
                    submission.getScorePayload(),
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Double.class)
            );
            return calculator.calculateForTarget(submission.getEvaluationType(), scores, studentId);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("INVALID_SCORE_PAYLOAD", "A locked evaluation sheet cannot be read");
        }
    }

    private Double first(Map<EvaluationType, Double> values, EvaluationType first, EvaluationType second) {
        return values.containsKey(first) ? values.get(first) : values.get(second);
    }

    private void updateProjectSummary(Project project, Phase phase, List<StudentPhaseGrade> values) {
        double average = values.stream().mapToDouble(StudentPhaseGrade::getFinalScore).average().orElse(0);
        Grade summary = projectGrades.findByProjectIdAndPhaseId(project.getId(), phase.getId()).orElseGet(Grade::new);
        summary.setProject(project);
        summary.setPhase(phase);
        summary.setPhaseType(phase.getType());
        summary.setRawScore(round(average));
        summary.setWeightedScore(round(average));
        summary.setFinalScore(round(average));
        projectGrades.save(summary);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
