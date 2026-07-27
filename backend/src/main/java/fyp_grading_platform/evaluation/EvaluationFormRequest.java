package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationFormRequest(@NotBlank String name, @NotNull EvaluationType evaluationType, @NotNull PhaseType phaseType, String description, double totalWeight) {}
