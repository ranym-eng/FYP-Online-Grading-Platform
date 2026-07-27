package fyp_grading_platform.project;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PhaseExtensionDecisionRequest(
        LocalDateTime extendedDeadline,
        @Size(max = 2000) String adminComment
) {}
