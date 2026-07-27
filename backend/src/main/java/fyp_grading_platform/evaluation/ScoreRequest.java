package fyp_grading_platform.evaluation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ScoreRequest(@NotNull UUID criterionId, double score, String comment) {}
