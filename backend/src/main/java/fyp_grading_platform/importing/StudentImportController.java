package fyp_grading_platform.importing;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Imports", description = "Administrative Excel and CSV imports")
public class StudentImportController {
    private final StudentImportService imports;
    private final CurrentUserService currentUsers;

    public StudentImportController(StudentImportService imports, CurrentUserService currentUsers) {
        this.imports = imports;
        this.currentUsers = currentUsers;
    }

    @PostMapping(value = "/students/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate and preview an official SQU student file without saving")
    ResponseEntity<ApiResponse<StudentImportReport>> preview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("file") MultipartFile file
    ) {
        currentUsers.requireAdmin(authorization);
        StudentImportReport report = imports.preview(file);
        return ResponseEntity.ok(ApiResponse.ok("Student file analyzed", report));
    }

    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import or update students from an official SQU Excel/CSV file")
    ResponseEntity<ApiResponse<StudentImportReport>> importStudents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("file") MultipartFile file
    ) {
        User actor = currentUsers.requireAdmin(authorization);
        StudentImportReport report = imports.importStudents(file, actor);
        if (!report.importable()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.fail("Student import validation failed", report));
        }
        return ResponseEntity.ok(ApiResponse.ok("Students imported", report));
    }
}