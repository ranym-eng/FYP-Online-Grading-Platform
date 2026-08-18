package fyp_grading_platform.importing;

public record InitializationSheetSummary(
        String sheet,
        int totalRows,
        int validRows,
        int created,
        int updated,
        int unchanged
) {}
