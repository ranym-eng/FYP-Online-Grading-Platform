package fyp_grading_platform.importing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Normalized preview of one official SQU student row")
public record StudentImportRow(
        int rowNumber,
        String studentNumber,
        String cohort,
        String fullName,
        String email,
        boolean existing,
        List<String> errors
) {}