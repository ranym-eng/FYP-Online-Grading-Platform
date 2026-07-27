package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.grading.GradeRepository;
import fyp_grading_platform.notification.EmailNotification;
import fyp_grading_platform.notification.EmailNotificationRepository;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReportingService {
    private final ReportRepository reports;
    private final ProjectRepository projects;
    private final PhaseRepository phases;
    private final GradeRepository grades;
    private final EmailNotificationRepository notifications;

    public ReportingService(ReportRepository reports, ProjectRepository projects, PhaseRepository phases, GradeRepository grades, EmailNotificationRepository notifications) { this.reports = reports; this.projects = projects; this.phases = phases; this.grades = grades; this.notifications = notifications; }

    public Report generate(UUID projectId, UUID phaseId) {
        var project = projects.findById(projectId).orElseThrow();
        var phase = phaseId == null ? null : phases.findById(phaseId).orElseThrow();
        String content = "FYP Report\nProject: " + project.getTitle() + "\nTrack: " + project.getTrack().getCode() + "\nPhase: " + (phase == null ? "FINAL" : phase.getName()) + "\nGrades: " + grades.findByProjectId(projectId).size();
        Report report = new Report();
        report.setProject(project); report.setPhase(phase); report.setGeneratedAt(LocalDateTime.now()); report.setStatus(ReportStatus.GENERATED); report.setRecipientEmail("fyp-coordinator@university.edu"); report.setContentSnapshot(content); report.setFilePath("generated/report-" + projectId + ".pdf");
        return reports.save(report);
    }

    public Report send(UUID id) {
        Report report = reports.findById(id).orElseThrow();
        EmailNotification email = new EmailNotification();
        email.setRecipient(report.getRecipientEmail()); email.setSubject("FYP Grade Report"); email.setBody(report.getContentSnapshot()); email.setAttachmentPath(report.getFilePath()); email.setStatus("SENT"); email.setSentAt(LocalDateTime.now());
        notifications.save(email);
        report.setStatus(ReportStatus.SENT); report.setSentAt(LocalDateTime.now()); return reports.save(report);
    }
}
