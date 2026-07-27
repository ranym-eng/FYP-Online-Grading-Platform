package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportRepository reports;
    private final ReportingService service;
    public ReportController(ReportRepository reports, ReportingService service) { this.reports = reports; this.service = service; }

    @PostMapping("/project/{projectId}/phase/{phaseId}") ApiResponse<?> generate(@PathVariable UUID projectId, @PathVariable UUID phaseId) { return ApiResponse.ok("Report generated", service.generate(projectId, phaseId)); }
    @PostMapping("/project/{projectId}/final") ApiResponse<?> generateFinal(@PathVariable UUID projectId) { return ApiResponse.ok("Final report generated", service.generate(projectId, null)); }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Reports", reports.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Report", reports.findById(id)); }
    @GetMapping("/project/{projectId}") ApiResponse<?> byProject(@PathVariable UUID projectId) { return ApiResponse.ok("Reports", reports.findByProjectId(projectId)); }
    @GetMapping("/status/{status}") ApiResponse<?> byStatus(@PathVariable ReportStatus status) { return ApiResponse.ok("Reports", reports.findByStatus(status)); }
    @PostMapping("/{id}/send") ApiResponse<?> send(@PathVariable UUID id) { return ApiResponse.ok("Report sent", service.send(id)); }
    @PostMapping("/{id}/regenerate") ApiResponse<?> regenerate(@PathVariable UUID id) { Report r = reports.findById(id).orElseThrow(); return ApiResponse.ok("Report regenerated", service.generate(r.getProject().getId(), r.getPhase() == null ? null : r.getPhase().getId())); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { reports.deleteById(id); return ApiResponse.ok("Report deleted", null); }
}
