package fyp_grading_platform.user;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EvaluatorRequest(
        @NotNull UUID userId,
        String department,
        String specialization,
        String externalOrganization,
        boolean external
) {}
