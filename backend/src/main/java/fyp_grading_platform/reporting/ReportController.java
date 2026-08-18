package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.User;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ReportingService service;
    private final ReportRepository reports;
    private final FinalResultsExportService exports;
    private final CurrentUserService currentUsers;

    public ReportController(
            ReportingService service,
            ReportRepository reports,
            FinalResultsExportService exports,
            CurrentUserService currentUsers
    ) {
        this.service = service;
        this.reports = reports;
        this.exports = exports;
        this.currentUsers = currentUsers;
    }

    @PostMapping("/project/{projectId}/phase/{phaseId}")
    ApiResponse<?> generate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Report generated", service.generate(projectId, phaseId));
    }

    @PostMapping("/project/{projectId}/final")
    ApiResponse<?> generateFinal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Final report generated", service.generate(projectId, null));
    }

    @GetMapping
    ApiResponse<?> all(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Reports", reports.findAll());
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Report", reports.findById(id));
    }

    @GetMapping("/project/{projectId}")
    ApiResponse<?> byProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Reports", reports.findByProjectId(projectId));
    }

    @GetMapping("/status/{status}")
    ApiResponse<?> byStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable ReportStatus status
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Reports", reports.findByStatus(status));
    }

    @GetMapping("/completeness/phase/{phaseId}")
    ApiResponse<?> completeness(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID phaseId
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Evaluation completeness", exports.completeness(phaseId));
    }

    @GetMapping("/export/phase/{phaseId}")
    ResponseEntity<byte[]> exportPhase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID phaseId
    ) {
        requireReportingRole(authorization);
        return workbook("Final_Evaluation_Summary_" + phaseId + ".xlsx", exports.generatePhase(phaseId));
    }

    @GetMapping("/export/project/{projectId}")
    ResponseEntity<byte[]> exportProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        requireReportingRole(authorization);
        return workbook("FYP_Project_Results_" + projectId + ".xlsx", exports.generateProject(projectId));
    }

    @PostMapping("/{id}/send")
    ApiResponse<?> send(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        requireReportingRole(authorization);
        return ApiResponse.ok("Report sent", service.send(id));
    }

    @PostMapping("/{id}/regenerate")
    ApiResponse<?> regenerate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        requireReportingRole(authorization);
        Report report = reports.findById(id).orElseThrow();
        return ApiResponse.ok(
                "Report regenerated",
                service.generate(report.getProject().getId(), report.getPhase() == null ? null : report.getPhase().getId())
        );
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        requireReportingRole(authorization);
        reports.deleteById(id);
        return ApiResponse.ok("Report deleted", null);
    }

    private ResponseEntity<byte[]> workbook(String filename, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private User requireReportingRole(String authorization) {
        User actor = currentUsers.requireUser(authorization);
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.COORDINATOR) {
            throw new BusinessException("REPORTING_ACCESS_DENIED", "Only administrators and coordinators can manage reports");
        }
        return actor;
    }
}
