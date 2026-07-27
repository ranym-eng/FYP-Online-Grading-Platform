package fyp_grading_platform.importing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Student spreadsheet validation and import result")
public record StudentImportReport(
        String sheetName,
        int totalRows,
        int validRows,
        int created,
        int updated,
        int unchanged,
        List<StudentImportError> errors,
        List<StudentImportRow> rows
) {
    public boolean importable() {
        return totalRows > 0 && errors.isEmpty();
    }
}