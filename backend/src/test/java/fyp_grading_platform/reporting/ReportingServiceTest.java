package fyp_grading_platform.reporting;

import fyp_grading_platform.common.ReportStatus;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.notification.EmailNotification;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {
    @Mock ReportRepository reports;
    @Mock ProjectRepository projects;
    @Mock PhaseRepository phases;
    @Mock FinalResultsExportService exports;
    @Mock EmailDeliveryService delivery;

    @Test
    void sendsTheArchivedExcelFileAsAnEmailAttachment() {
        UUID reportId = UUID.randomUUID();
        Project project = new Project();
        project.setProjectNumber("PSE-01");
        Report report = new Report();
        report.setId(reportId);
        report.setProject(project);
        report.setRecipientEmail("fyp-coordinator@squ.edu.om");
        report.setContentSnapshot("Final project results");
        report.setFilePath("C:/generated/PSE-01-FINAL.xlsx");
        EmailNotification sent = new EmailNotification();
        sent.setStatus("SENT");
        when(reports.findById(reportId)).thenReturn(Optional.of(report));
        when(delivery.send(
                report.getRecipientEmail(),
                "FYP grade report - PSE-01",
                report.getContentSnapshot(),
                report.getFilePath()
        )).thenReturn(sent);
        when(reports.save(report)).thenReturn(report);
        ReportingService service = new ReportingService(
                reports, projects, phases, exports, delivery,
                "generated", "fyp-coordinator@squ.edu.om"
        );

        Report result = service.send(reportId);

        assertEquals(ReportStatus.SENT, result.getStatus());
        assertNotNull(result.getSentAt());
        verify(delivery).send(
                eq("fyp-coordinator@squ.edu.om"),
                eq("FYP grade report - PSE-01"),
                eq("Final project results"),
                eq("C:/generated/PSE-01-FINAL.xlsx")
        );
    }
}
