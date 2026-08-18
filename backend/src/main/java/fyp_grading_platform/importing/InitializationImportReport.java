package fyp_grading_platform.importing;

import java.util.List;

public record InitializationImportReport(
        boolean preview,
        boolean importable,
        int totalRows,
        int validRows,
        List<InitializationSheetSummary> sheets,
        List<InitializationImportError> errors
) {}
