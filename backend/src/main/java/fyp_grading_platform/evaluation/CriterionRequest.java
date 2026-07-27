package fyp_grading_platform.evaluation;

import jakarta.validation.constraints.NotBlank;

public record CriterionRequest(@NotBlank String title, String description, double maxScore, double weight, int displayOrder, boolean required) {}
