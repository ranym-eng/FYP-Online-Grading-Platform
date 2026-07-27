package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record EvaluationDraftRequest(@NotNull UUID projectId, @NotNull UUID phaseId, @NotNull UUID formTemplateId, @NotNull UUID evaluatorId, @NotNull EvaluationType evaluationType, String generalComment, List<ScoreRequest> scores) {}
