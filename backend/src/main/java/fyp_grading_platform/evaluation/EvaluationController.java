package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.project.ProjectAccessService;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationSubmissionRepository submissions;
    private final CriterionScoreRepository scores;
    private final EvaluationService service;
    private final CurrentUserService currentUsers;
    private final ProjectAccessService projectAccess;

    public EvaluationController(
            EvaluationSubmissionRepository submissions,
            CriterionScoreRepository scores,
            EvaluationService service,
            CurrentUserService currentUsers,
            ProjectAccessService projectAccess
    ) {
        this.submissions = submissions;
        this.scores = scores;
        this.service = service;
        this.currentUsers = currentUsers;
        this.projectAccess = projectAccess;
    }

    @PostMapping("/draft")
    ApiResponse<?> draft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EvaluationDraftRequest request
    ) {
        return ApiResponse.ok(
                "Draft saved",
                service.saveDraft(request, null, currentUsers.requireUser(authorization))
        );
    }

    @PutMapping("/{submissionId}/draft")
    ApiResponse<?> updateDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId,
            @Valid @RequestBody EvaluationDraftRequest request
    ) {
        return ApiResponse.ok(
                "Draft updated",
                service.saveDraft(request, submissionId, currentUsers.requireUser(authorization))
        );
    }

    @PostMapping("/sheet/draft")
    ApiResponse<?> sheetDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EvaluationSheetDraftRequest request
    ) {
        return ApiResponse.ok(
                "Score sheet draft saved",
                service.saveSheetDraft(request, null, currentUsers.requireUser(authorization))
        );
    }

    @PutMapping("/{submissionId}/sheet/draft")
    ApiResponse<?> updateSheetDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId,
            @Valid @RequestBody EvaluationSheetDraftRequest request
    ) {
        return ApiResponse.ok(
                "Score sheet draft updated",
                service.saveSheetDraft(request, submissionId, currentUsers.requireUser(authorization))
        );
    }

    @GetMapping("/sheet/current")
    ApiResponse<?> currentSheet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam UUID projectId,
            @RequestParam UUID phaseId,
            @RequestParam UUID evaluatorId,
            @RequestParam EvaluationType evaluationType
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok(
                "Current score sheet",
                service.currentSheet(projectId, phaseId, evaluatorId, evaluationType, actor)
        );
    }

    @PostMapping("/{submissionId}/submit")
    ApiResponse<?> submit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId
    ) {
        return ApiResponse.ok(
                "Evaluation submitted and locked",
                service.submit(submissionId, currentUsers.requireUser(authorization))
        );
    }

    @PostMapping("/{submissionId}/lock")
    ApiResponse<?> lock(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId
    ) {
        return ApiResponse.ok(
                "Evaluation locked",
                service.submit(submissionId, currentUsers.requireUser(authorization))
        );
    }

    @GetMapping("/{submissionId}")
    ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId
    ) {
        User actor = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Evaluation", requireVisibleSubmission(submissionId, actor));
    }

    @GetMapping("/{submissionId}/scores")
    ApiResponse<?> submissionScores(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID submissionId
    ) {
        User actor = currentUsers.requireUser(authorization);
        requireVisibleSubmission(submissionId, actor);
        return ApiResponse.ok("Scores", scores.findBySubmissionId(submissionId));
    }

    @GetMapping("/by-project/{projectId}")
    ApiResponse<?> byProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok("Evaluations", visibleSubmissions(submissions.findByProjectId(projectId), actor));
    }

    @GetMapping("/by-evaluator/{evaluatorId}")
    ApiResponse<?> byEvaluator(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID evaluatorId
    ) {
        User actor = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Evaluations", visibleSubmissions(submissions.findByEvaluatorId(evaluatorId), actor));
    }

    @GetMapping("/by-project/{projectId}/phase/{phaseId}")
    ApiResponse<?> byPhase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok(
                "Evaluations",
                visibleSubmissions(submissions.findByProjectIdAndPhaseId(projectId, phaseId), actor)
        );
    }

    @GetMapping("/by-project/{projectId}/type/{type}")
    ApiResponse<?> byType(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable EvaluationType type
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok(
                "Evaluations",
                visibleSubmissions(submissions.findByProjectIdAndEvaluationType(projectId, type), actor)
        );
    }

    @GetMapping("/progress/project/{projectId}")
    ApiResponse<?> progress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok("Progress", progressData(projectId, actor));
    }

    @GetMapping("/status/project/{projectId}")
    ApiResponse<?> status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok("Progress", progressData(projectId, actor));
    }

    private EvaluationSubmission requireVisibleSubmission(UUID submissionId, User actor) {
        EvaluationSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", "Evaluation submission not found"));
        projectAccess.assertCanView(actor, submission.getProject().getId());
        if (!projectAccess.canViewAll(actor) && !belongsToActor(submission, actor)) {
            throw new BusinessException("EVALUATION_ACCESS_DENIED", "This evaluation does not belong to your account");
        }
        return submission;
    }

    private List<EvaluationSubmission> visibleSubmissions(List<EvaluationSubmission> candidates, User actor) {
        if (projectAccess.canViewAll(actor)) return candidates;
        return candidates.stream()
                .filter(submission -> submission.getProject() != null
                        && projectAccess.canView(actor, submission.getProject().getId())
                        && belongsToActor(submission, actor))
                .toList();
    }

    private boolean belongsToActor(EvaluationSubmission submission, User actor) {
        return submission.getEvaluator() != null
                && submission.getEvaluator().getUser() != null
                && submission.getEvaluator().getUser().getId().equals(actor.getId());
    }

    private Map<String, Object> progressData(UUID projectId, User actor) {
        List<EvaluationSubmission> visible = visibleSubmissions(submissions.findByProjectId(projectId), actor);
        long locked = visible.stream().filter(EvaluationSubmission::isLocked).count();
        return Map.of(
                "totalSubmissions", visible.size(),
                "lockedSubmissions", locked,
                "completionPercentage", visible.isEmpty() ? 0 : locked * 100.0 / visible.size()
        );
    }
}