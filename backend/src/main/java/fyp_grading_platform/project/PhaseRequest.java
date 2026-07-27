package fyp_grading_platform.project;

import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.PhaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PhaseRequest(
        @NotNull PhaseType type,
        @NotBlank String name,
        @NotBlank String academicYear,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime deadline,
        PhaseStatus status
) {}