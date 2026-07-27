package fyp_grading_platform.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhaseExtensionCreateRequest(
        @NotNull UUID phaseId,
        @NotBlank @Size(max = 2000) String reason,
        LocalDateTime requestedDeadline
) {}
