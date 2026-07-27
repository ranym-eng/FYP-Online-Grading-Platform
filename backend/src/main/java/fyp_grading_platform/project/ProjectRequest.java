package fyp_grading_platform.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "FYP project data, including the number printed on evaluation forms")
public record ProjectRequest(
        @NotBlank String projectNumber,
        @NotBlank String title,
        String abstractText,
        @NotBlank String academicYear,
        @NotNull UUID trackId,
        String status
) {}