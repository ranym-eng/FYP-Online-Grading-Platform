package fyp_grading_platform.user;

import jakarta.validation.constraints.NotBlank;

public record StudentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String studentNumber,
        @NotBlank String academicYear,
        String trackCode,
        String level
) {}
