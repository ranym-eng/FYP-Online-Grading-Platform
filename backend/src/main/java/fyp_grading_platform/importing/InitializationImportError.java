package fyp_grading_platform.importing;

public record InitializationImportError(
        String sheet,
        int rowNumber,
        String field,
        String value,
        String message
) {}
