package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReportingService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ReportRepository reports;
    private final ProjectRepository projects;
    private final PhaseRepository phases;
    private final FinalResultsExportService exports;
    private final EmailDeliveryService delivery;
    private final Path outputDirectory;
    private final String recipientEmail;

    public ReportingService(
            ReportRepository reports,
            ProjectRepository projects,
            PhaseRepository phases,
            FinalResultsExportService exports,
            EmailDeliveryService delivery,
            @Value("${app.reports.output-dir:generated}") String outputDirectory,
            @Value("${app.reports.recipient-email:fyp-coordinator@squ.edu.om}") String recipientEmail
    ) {
        this.reports = reports;
        this.projects = projects;
        this.phases = phases;
        this.exports = exports;
        this.delivery = delivery;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.recipientEmail = recipientEmail;
    }

    public Report generate(UUID projectId, UUID phaseId) {
        var project = projects.findById(projectId)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        var phase = phaseId == null ? null : phases.findById(phaseId)
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        byte[] content = phase == null ? exports.generateProject(projectId) : exports.generateProjectPhase(projectId, phaseId);
        String suffix = phase == null ? "FINAL" : phase.getType().name();
        String filename = safe(project.getProjectNumber()) + "-" + suffix + "-"
                + FILE_TIME.format(LocalDateTime.now()) + ".xlsx";
        Path file = outputDirectory.resolve(filename).normalize();
        if (!file.startsWith(outputDirectory)) {
            throw new BusinessException("INVALID_REPORT_PATH", "Invalid report output path");
        }
        try {
            Files.createDirectories(outputDirectory);
            Files.write(file, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new BusinessException("REPORT_WRITE_FAILED", "The Excel report could not be written");
        }

        Report report = new Report();
        report.setProject(project);
        report.setPhase(phase);
        report.setGeneratedAt(LocalDateTime.now());
        report.setStatus(ReportStatus.GENERATED);
        report.setRecipientEmail(recipientEmail);
        report.setContentSnapshot(
                "FYP results for " + project.getProjectNumber() + " - " + suffix
                        + ". The report is available for authenticated download in the FYP platform."
        );
        report.setFilePath(file.toString());
        return reports.save(report);
    }

    public Report send(UUID id) {
        Report report = reports.findById(id)
                .orElseThrow(() -> new BusinessException("REPORT_NOT_FOUND", "Report not found"));
        var notification = delivery.send(
                report.getRecipientEmail(),
                "FYP grade report - " + report.getProject().getProjectNumber(),
                report.getContentSnapshot(),
                null
        );
        if ("SENT".equalsIgnoreCase(notification.getStatus())) {
            report.setStatus(ReportStatus.SENT);
            report.setSentAt(LocalDateTime.now());
        } else {
            report.setStatus(ReportStatus.FAILED);
        }
        return reports.save(report);
    }

    private String safe(String value) {
        return value == null ? "project" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
