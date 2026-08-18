package fyp_grading_platform.reporting;

import fyp_grading_platform.common.EvaluationType;

import java.util.UUID;

public record EvaluationCompletenessRow(
        UUID projectId,
        String projectNumber,
        String projectTitle,
        EvaluationType evaluationType,
        String evaluatorName,
        String evaluatorEmail,
        String status
) {
}
