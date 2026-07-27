package fyp_grading_platform.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.SubmissionStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.PhaseWindowService;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.user.EvaluatorProfile;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EvaluationService {
    private final EvaluationSubmissionRepository submissions;
    private final CriterionScoreRepository scores;
    private final RubricCriterionRepository criteria;
    private final ProjectRepository projects;
    private final TeamRepository teams;
    private final PhaseRepository phases;
    private final EvaluationFormTemplateRepository forms;
    private final EvaluatorProfileRepository evaluators;
    private final ProjectEvaluatorAssignmentRepository assignments;
    private final PhaseWindowService phaseWindows;
    private final EvaluationSheetCalculator sheetCalculator;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            EvaluationSubmissionRepository submissions,
            CriterionScoreRepository scores,
            RubricCriterionRepository criteria,
            ProjectRepository projects,
            TeamRepository teams,
            PhaseRepository phases,
            EvaluationFormTemplateRepository forms,
            EvaluatorProfileRepository evaluators,
            ProjectEvaluatorAssignmentRepository assignments,
            PhaseWindowService phaseWindows,
            EvaluationSheetCalculator sheetCalculator,
            ObjectMapper objectMapper
    ) {
        this.submissions = submissions;
        this.scores = scores;
        this.criteria = criteria;
        this.projects = projects;
        this.teams = teams;
        this.phases = phases;
        this.forms = forms;
        this.evaluators = evaluators;
        this.assignments = assignments;
        this.phaseWindows = phaseWindows;
        this.sheetCalculator = sheetCalculator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvaluationSubmission saveDraft(EvaluationDraftRequest request, UUID existingId, User actor) {
        Phase phase = phases.findById(request.phaseId())
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        EvaluatorProfile evaluator = evaluators.findById(request.evaluatorId())
                .orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        assertEvaluatorIdentity(evaluator, actor);
        phaseWindows.assertEvaluationAllowed(phase, actor);
        assertAssignment(request.projectId(), request.evaluatorId(), request.evaluationType());

        EvaluationSubmission submission = existingId == null ? new EvaluationSubmission()
                : submissions.findById(existingId)
                    .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", "Submission not found"));
        if (submission.isLocked()) {
            throw new BusinessException("EVALUATION_LOCKED", "Evaluation is already locked");
        }
        if (existingId != null) assertEvaluatorIdentity(submission.getEvaluator(), actor);

        submission.setProject(projects.findById(request.projectId())
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found")));
        submission.setPhase(phase);
        submission.setFormTemplate(forms.findById(request.formTemplateId())
                .orElseThrow(() -> new BusinessException("FORM_NOT_FOUND", "Evaluation form not found")));
        submission.setEvaluator(evaluator);
        submission.setEvaluationType(request.evaluationType());
        submission.setGeneralComment(request.generalComment());
        submission.setStatus(SubmissionStatus.DRAFT);
        submission.setDraftSavedAt(LocalDateTime.now());
        EvaluationSubmission saved = submissions.save(submission);
        replaceScores(saved, request.scores());
        saved.setTotalScore(calculateTotal(saved.getId()));
        return submissions.save(saved);
    }

    @Transactional
    public EvaluationSubmission saveSheetDraft(
            EvaluationSheetDraftRequest request,
            UUID existingId,
            User actor
    ) {
        Phase phase = phases.findById(request.phaseId())
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        EvaluatorProfile evaluator = evaluators.findById(request.evaluatorId())
                .orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        assertEvaluatorIdentity(evaluator, actor);
        phaseWindows.assertEvaluationAllowed(phase, actor);
        assertAssignment(request.projectId(), request.evaluatorId(), request.evaluationType());
        var project = projects.findById(request.projectId())
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        Set<String> expectedScoreKeys = sheetCalculator.expectedScoreKeys(
                request.evaluationType(),
                studentIds(request.projectId())
        );
        sheetCalculator.validateScoreKeys(request.evaluationType(), request.scores(), studentIds(request.projectId()));

        EvaluationSubmission submission;
        if (existingId != null) {
            submission = submissions.findById(existingId)
                    .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", "Submission not found"));
        } else {
            submission = submissions
                    .findFirstByProjectIdAndPhaseIdAndEvaluatorIdAndEvaluationTypeOrderByCreatedAtDesc(
                            request.projectId(),
                            request.phaseId(),
                            request.evaluatorId(),
                            request.evaluationType()
                    )
                    .filter(current -> !current.isLocked())
                    .orElseGet(EvaluationSubmission::new);
        }
        if (submission.isLocked()) {
            throw new BusinessException("EVALUATION_LOCKED", "Evaluation is already locked");
        }
        if (submission.getEvaluator() != null) assertEvaluatorIdentity(submission.getEvaluator(), actor);

        submission.setProject(project);
        submission.setPhase(phase);
        submission.setFormTemplate(forms.findFirstByEvaluationTypeAndActiveTrue(request.evaluationType())
                .orElseThrow(() -> new BusinessException("FORM_NOT_FOUND", "No active evaluation form was found")));
        submission.setEvaluator(evaluator);
        submission.setEvaluationType(request.evaluationType());
        submission.setGeneralComment(request.generalComment());
        submission.setScorePayload(writeScores(request.scores()));
        submission.setRequiredScoreCount(expectedScoreKeys.size());
        submission.setCompletedScoreCount(request.scores().size());
        submission.setTotalScore(sheetCalculator.calculate(request.evaluationType(), request.scores()));
        submission.setStatus(SubmissionStatus.DRAFT);
        submission.setDraftSavedAt(LocalDateTime.now());
        return submissions.save(submission);
    }

    @Transactional(readOnly = true)
    public EvaluationSubmission currentSheet(
            UUID projectId,
            UUID phaseId,
            UUID evaluatorId,
            EvaluationType evaluationType,
            User actor
    ) {
        EvaluatorProfile evaluator = evaluators.findById(evaluatorId)
                .orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        assertEvaluatorIdentity(evaluator, actor);
        return submissions
                .findFirstByProjectIdAndPhaseIdAndEvaluatorIdAndEvaluationTypeOrderByCreatedAtDesc(
                        projectId,
                        phaseId,
                        evaluatorId,
                        evaluationType
                )
                .orElse(null);
    }

    @Transactional
    public EvaluationSubmission submit(UUID id, User actor) {
        EvaluationSubmission submission = submissions.findById(id)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", "Submission not found"));
        assertEvaluatorIdentity(submission.getEvaluator(), actor);
        phaseWindows.assertEvaluationAllowed(submission.getPhase(), actor);
        if (submission.isLocked()) {
            throw new BusinessException("EVALUATION_LOCKED", "Evaluation is already locked");
        }

        if (submission.getScorePayload() != null) {
            Map<String, Double> currentScores = readScores(submission.getScorePayload());
            Set<String> expected = sheetCalculator.expectedScoreKeys(
                    submission.getEvaluationType(),
                    studentIds(submission.getProject().getId())
            );
            sheetCalculator.validateScoreKeys(
                    submission.getEvaluationType(),
                    currentScores,
                    studentIds(submission.getProject().getId())
            );
            if (!currentScores.keySet().containsAll(expected)) {
                throw new BusinessException(
                        "MISSING_REQUIRED_CRITERION",
                        "All required scores must be entered before the form is submitted"
                );
            }
            submission.setRequiredScoreCount(expected.size());
            submission.setCompletedScoreCount(expected.size());
            submission.setTotalScore(sheetCalculator.calculate(submission.getEvaluationType(), currentScores));
        } else {
            assertLegacyCriteriaComplete(submission);
        }

        LocalDateTime now = LocalDateTime.now();
        if (submission.getScorePayload() == null) {
            submission.setTotalScore(calculateTotal(id));
        }
        submission.setStatus(SubmissionStatus.LOCKED);
        submission.setLocked(true);
        submission.setSubmittedAt(now);
        submission.setLockedAt(now);
        return submissions.save(submission);
    }

    private void assertLegacyCriteriaComplete(EvaluationSubmission submission) {
        List<RubricCriterion> required = criteria
                .findByFormTemplateIdOrderByDisplayOrderAsc(submission.getFormTemplate().getId())
                .stream()
                .filter(RubricCriterion::isRequired)
                .toList();
        List<CriterionScore> current = scores.findBySubmissionId(submission.getId());
        for (RubricCriterion criterion : required) {
            boolean exists = current.stream()
                    .anyMatch(score -> score.getCriterion().getId().equals(criterion.getId()));
            if (!exists) {
                throw new BusinessException(
                        "MISSING_REQUIRED_CRITERION",
                        "A required criterion score is missing"
                );
            }
        }
    }

    private void assertEvaluatorIdentity(EvaluatorProfile evaluator, User actor) {
        if (actor.getRole() != UserRole.ADMIN && !evaluator.getUser().getId().equals(actor.getId())) {
            throw new BusinessException(
                    "EVALUATOR_IDENTITY_MISMATCH",
                    "You can modify only your own evaluations"
            );
        }
    }

    private void assertAssignment(UUID projectId, UUID evaluatorId, EvaluationType type) {
        if (!assignments.existsByProjectIdAndEvaluatorIdAndEvaluationTypeAndActiveTrue(
                projectId,
                evaluatorId,
                type
        )) {
            throw new BusinessException(
                    "EVALUATOR_NOT_ASSIGNED",
                    "Evaluator is not assigned to this project and evaluation type"
            );
        }
    }

    private Set<String> studentIds(UUID projectId) {
        return teams.findByProjectId(projectId)
                .map(team -> team.getStudents().stream()
                        .map(student -> student.getId().toString())
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)))
                .orElseGet(java.util.LinkedHashSet::new);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> readScores(String payload) {
        try {
            return objectMapper.readValue(payload, objectMapper.getTypeFactory()
                    .constructMapType(java.util.LinkedHashMap.class, String.class, Double.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("INVALID_SCORE_PAYLOAD", "The stored score sheet could not be read");
        }
    }

    private String writeScores(Map<String, Double> scoreValues) {
        try {
            return objectMapper.writeValueAsString(scoreValues);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("INVALID_SCORE_PAYLOAD", "The score sheet could not be saved");
        }
    }

    private void replaceScores(EvaluationSubmission submission, List<ScoreRequest> requests) {
        scores.deleteBySubmissionId(submission.getId());
        if (requests == null) return;
        for (ScoreRequest request : requests) {
            RubricCriterion criterion = criteria.findById(request.criterionId())
                    .orElseThrow(() -> new BusinessException("CRITERION_NOT_FOUND", "Rubric criterion not found"));
            if (request.score() < 0 || request.score() > criterion.getMaxScore()) {
                throw new BusinessException("INVALID_SCORE", "Score must be between 0 and maxScore");
            }
            CriterionScore score = new CriterionScore();
            score.setSubmission(submission);
            score.setCriterion(criterion);
            score.setScore(request.score());
            score.setComment(request.comment());
            scores.save(score);
        }
    }

    public double calculateTotal(UUID submissionId) {
        List<CriterionScore> current = scores.findBySubmissionId(submissionId);
        double weighted = current.stream()
                .mapToDouble(score -> score.getScore() * score.getCriterion().getWeight())
                .sum();
        double weights = current.stream()
                .mapToDouble(score -> score.getCriterion().getWeight())
                .sum();
        return weights == 0 ? 0 : weighted / weights;
    }
}