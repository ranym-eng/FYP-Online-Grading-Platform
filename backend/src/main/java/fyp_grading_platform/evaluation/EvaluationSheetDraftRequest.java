package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record EvaluationSheetDraftRequest(
        @NotNull UUID projectId,
        @NotNull UUID phaseId,
        @NotNull UUID evaluatorId,
        @NotNull EvaluationType evaluationType,
        String generalComment,
        @NotNull Map<String, Double> scores,
        @Min(1) int requiredScoreCount
) {}
