package fyp_grading_platform.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StudentRequest(
        @NotNull UUID userId,
        @NotBlank String studentNumber,
        @NotBlank String academicYear,
        String trackCode,
        String level
) {}
