package fyp_grading_platform.project;

import jakarta.validation.constraints.NotBlank;

public record TrackRequest(@NotBlank String code, @NotBlank String name, String description) {}
