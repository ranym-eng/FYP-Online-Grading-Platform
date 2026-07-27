package fyp_grading_platform.importing;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Validation problem found on one imported row")
public record StudentImportError(
        int rowNumber,
        String field,
        String value,
        String message
) {}