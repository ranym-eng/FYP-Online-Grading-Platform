package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EvaluatorAssignmentRequest(@NotNull UUID evaluatorId, @NotNull EvaluationType evaluationType) {}
