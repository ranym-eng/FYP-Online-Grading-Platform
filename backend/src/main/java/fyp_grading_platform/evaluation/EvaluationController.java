package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.security.CurrentUserService;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationSubmissionRepository submissions;
    private final CriterionScoreRepository scores;
    private final EvaluationService service;
    private final CurrentUserService currentUsers;

    public EvaluationController(
            EvaluationSubmissionRepository submissions,
            CriterionScoreRepository scores,
            EvaluationService service,
            CurrentUserService currentUsers
    ) {
        this.submissions = submissions;
        this.scores = scores;
        this.service = service;
        this.currentUsers = currentUsers;
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
        return ApiResponse.ok(
                "Current score sheet",
                service.currentSheet(
                        projectId,
                        phaseId,
                        evaluatorId,
                        evaluationType,
                        currentUsers.requireUser(authorization)
                )
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
    ApiResponse<?> one(@PathVariable UUID submissionId) {
        return ApiResponse.ok("Evaluation", submissions.findById(submissionId));
    }

    @GetMapping("/{submissionId}/scores")
    ApiResponse<?> submissionScores(@PathVariable UUID submissionId) {
        return ApiResponse.ok("Scores", scores.findBySubmissionId(submissionId));
    }

    @GetMapping("/by-project/{projectId}")
    ApiResponse<?> byProject(@PathVariable UUID projectId) {
        return ApiResponse.ok("Evaluations", submissions.findByProjectId(projectId));
    }

    @GetMapping("/by-evaluator/{evaluatorId}")
    ApiResponse<?> byEvaluator(@PathVariable UUID evaluatorId) {
        return ApiResponse.ok("Evaluations", submissions.findByEvaluatorId(evaluatorId));
    }

    @GetMapping("/by-project/{projectId}/phase/{phaseId}")
    ApiResponse<?> byPhase(@PathVariable UUID projectId, @PathVariable UUID phaseId) {
        return ApiResponse.ok("Evaluations", submissions.findByProjectIdAndPhaseId(projectId, phaseId));
    }

    @GetMapping("/by-project/{projectId}/type/{type}")
    ApiResponse<?> byType(@PathVariable UUID projectId, @PathVariable EvaluationType type) {
        return ApiResponse.ok("Evaluations", submissions.findByProjectIdAndEvaluationType(projectId, type));
    }

    @GetMapping("/progress/project/{projectId}")
    ApiResponse<?> progress(@PathVariable UUID projectId) {
        var list = submissions.findByProjectId(projectId);
        long locked = list.stream().filter(EvaluationSubmission::isLocked).count();
        return ApiResponse.ok(
                "Progress",
                Map.of(
                        "totalSubmissions", list.size(),
                        "lockedSubmissions", locked,
                        "completionPercentage", list.isEmpty() ? 0 : locked * 100.0 / list.size()
                )
        );
    }

    @GetMapping("/status/project/{projectId}")
    ApiResponse<?> status(@PathVariable UUID projectId) {
        return progress(projectId);
    }
}