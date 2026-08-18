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
@RequestMapping("/api/import/initialization")
@Tag(name = "Platform initialization", description = "Validated, transactional import of the annual FYP dataset")
public class PlatformInitializationImportController {
    private final PlatformInitializationImportService legacyImports;
    private final SimplifiedInitializationImportService simplifiedImports;
    private final CurrentUserService currentUsers;

    public PlatformInitializationImportController(
            PlatformInitializationImportService legacyImports,
            SimplifiedInitializationImportService simplifiedImports,
            CurrentUserService currentUsers
    ) {
        this.legacyImports = legacyImports;
        this.simplifiedImports = simplifiedImports;
        this.currentUsers = currentUsers;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate the complete initialization workbook without saving")
    ResponseEntity<ApiResponse<InitializationImportReport>> preview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("file") MultipartFile file
    ) {
        currentUsers.requireAdmin(authorization);
        InitializationImportReport report = simplifiedImports.supports(file)
                ? simplifiedImports.preview(file)
                : legacyImports.preview(file);
        return ResponseEntity.ok(ApiResponse.ok("Initialization workbook analyzed", report));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import the complete annual FYP dataset in one transaction")
    ResponseEntity<ApiResponse<InitializationImportReport>> importWorkbook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("file") MultipartFile file
    ) {
        User actor = currentUsers.requireAdmin(authorization);
        InitializationImportReport report = simplifiedImports.supports(file)
                ? simplifiedImports.importWorkbook(file, actor)
                : legacyImports.importWorkbook(file, actor);
        if (!report.importable()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.fail("Initialization import validation failed", report));
        }
        return ResponseEntity.ok(ApiResponse.ok("Platform initialized from workbook", report));
    }
}
