package fyp_grading_platform.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record TeamRequest(@NotBlank String name, String section, @NotBlank String academicYear, @NotNull UUID projectId, List<UUID> studentIds) {}
