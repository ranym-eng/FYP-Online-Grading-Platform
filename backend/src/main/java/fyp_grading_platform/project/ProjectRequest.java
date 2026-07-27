package fyp_grading_platform.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProjectRequest(@NotBlank String title, String abstractText, @NotBlank String academicYear, @NotNull UUID trackId, String status) {}
